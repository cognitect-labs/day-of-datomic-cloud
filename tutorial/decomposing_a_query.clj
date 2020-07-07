;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local :system "datomic-samples"}))
(def conn (d/connect client {:db-name "decomposing-a-query"}))

(def db (d/db conn))

;; This query is intended to be gibberish.  Knowing nothing about the
;; domain or the data, how would you figure out why this query is
;; slow?
@(def e (ffirst (d/q '[:find (rand ?e)
                       :where [?e :a]]
                     db)))
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $ ?eid
                            :where
                            [?e1 :a ?v1]
                            [?e2 :a ?v2]
                            [?e1 :a ?eid]
                            [?e2 :a ?e1]]
                   :args [db e]})))
;; ~5 seconds, 1 result

;; Drop clauses from the end one at a time
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $ ?eid
                            :where
                            [?e1 :a ?v1]
                            [?e2 :a ?v2]
                            [?e1 :a ?eid]]
                   :args [db e]})))
;; ~4 seconds, 1200 results

;; Drop another clause
(time (count (d/q {:query '[:find ?e1 ?e2
                            :in $
                            :where
                            [?e1 :a ?v1]
                            [?e2 :a ?v2]]
                   :args [db]})))
;; 7 seconds, 1,440,000 results


;; You may need to adjust :find to remove variables no longer in the query
(time (count (d/q {:query '[:find ?e1
                            :in $
                            :where
                            [?e1 :a ?v1]]
                   :args [db]})))
;; 0.01 seconds, 1200 results


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
                   :args [db e]})))
;; 0.02 seconds, 1 result

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
      :args [db e]
      :timeout 10000})
