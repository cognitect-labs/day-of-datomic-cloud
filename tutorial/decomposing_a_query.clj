;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl])
(import '(java.util UUID))

(def client-cfg (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "scratch-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(set! *print-length* 100)

(d/transact conn {:tx-data
                  [{:db/ident :a
                    :db/cardinality :db.cardinality/one
                    :db/valueType :db.type/ref}
                   {:db/ident :b
                    :db/cardinality :db.cardinality/one
                    :db/valueType :db.type/ref}
                   {:db/ident :c
                    :db/cardinality :db.cardinality/one
                    :db/valueType :db.type/ref}
                   {:db/ident :d
                    :db/cardinality :db.cardinality/one
                    :db/valueType :db.type/ref}
                   {:db/ident :e
                    :db/cardinality :db.cardinality/one
                    :db/valueType :db.type/ref}]})

;; create a database of 6000 datoms
(def kvs (into [] (for [e (range 1200)
                        a [:a :b :c :d :e]]
                    [:db/add (str e) a (str e)])))
(count kvs)
;; transact the data (this can take a few seconds):
(def tx-result (d/transact conn {:tx-data kvs}))
;; get one of the transacted entities ids from the tempid map for use later:
(def ten
  (-> tx-result :tempids (get "10")))

(def db (d/db conn))

;; This query is intended to be gibberish.  Knowing nothing about the
;; domain or the data, how would you figure out why this query is slow?
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $ ?eid
                            :where
                            [?e1 :a ?v1]
                            [?e2 :a ?v2]
                            [?e1 :a ?eid]
                            [?e2 :a ?e1]]
                   :args [db ten]
                   :timeout 10000})))
;; ~5 seconds, 1 result

;; Drop clauses from the end one at a time
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $ ?eid
                            :where
                            [?e1 :a ?v1]
                            [?e2 :a ?v2]
                            [?e1 :a ?eid]]
                   :args [db ten]
                   :timeout 10000})))
;; ~4 seconds, 1200 results

;; Drop another clause
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $
                            :where
                            [?e1 :a ?v1]
                            [?e2 :a ?v2]]
                   :args [db]
                   :timeout 30000})))
;; 77 seconds, 1,440,000 results


;; You may need to adjust :find to remove variables no longer in the query
(time (count (d/q {:query '[:find ?e1
                            :in $
                            :where
                            [?e1 :a ?v1]]
                   :args [db]
                   :timeout 10000})))
;; 0.08 seconds, 1200 results


;; Looking back, the addition of the second clause blew out the number
;; of intermediate results and the query time. what if we change the
;; order and move that clause last?
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $ ?eid
                            :where
                            [?e1 :a ?v1]
                            [?e1 :a ?eid]
                            [?e2 :a ?e1]
                            [?e2 :a ?v2]]
                   :args [db ten]
                   :timeout 10000})))
;; 0.06 seconds, 1 result

;; Of course you can write a much better query by *understanding* its
;; intention (shown below). But it is interesting that you don't *have* to
;; understand the query to find problems by reordering or removing
;; clauses. You could even write a program to do it for you.


;; Anayzing the Query based on domain knowledge

;; 1. Lots of datoms match [?e1 :a ?v1]
;; 2. Lots of daotms match [?e2 :a ?v2]
;; 3. 1 & 2 have no vars in common, so this makes a cross-product. Danger!
;; 4. Very few datoms match [?e1 :a 10]
;; 5. Given 4, 1 is not needed at all
;; 6. Very few datoms match [?e2 :a ?e1]
;; 7. Given 6, 2 is not needed at all
;; 8. Given 4 & 6, ?e1 is known at input time
;; So:
(d/q {:query '[:find ?e1 ?e2
               :in $ ?e1
               :where [?e2 :a ?e1]]
      :args [db ten]
      :timeout 10000})

(d/delete-database client {:db-name db-name})
