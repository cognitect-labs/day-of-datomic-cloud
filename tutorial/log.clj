;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local
                       :system "datomic-samples"}))
(def conn (d/connect client {:db-name "streets"}))

(def db (d/db conn))

;; What do we know about Joe now?
(d/pull db '[*] [:person/name "Joe"])

;; Find all the places Joe has lived
(d/q '[:find ?sname
       :in $ ?name
       :where
       [?e :person/name ?name]
       [?e :person/street ?sname]]
     (d/history db) "Joe")

;; When did Joe move to Broadway?
;; This query will retrieve the transaction ID of the
;; transaction that asserted Joe's street is Broadway
(def tx-id
  (ffirst (d/q '[:find ?tx
                 :in $ ?name ?street
                 :where
                 [?e :person/name ?name]
                 [?e :person/street ?street ?tx true]]
               (d/history db) "Joe" "Broadway")))

;; Let's also find the wall clock time of that transaction
(d/pull db '[:db/txInstant] tx-id)

;; What else happened at the same time (i.e. during the same transaction)
;; as Joe moving to Broadway?
(-> (d/tx-range conn {:start tx-id :end (inc tx-id)})
    first :data)

;; Note that we see the same wall clock time we just queried for, as well
;; as 4 other datoms. One is the assertion of Joe moving to Broadway.
;; One is the retraction of his previous street (1st)

;; It also looks like another entity moved from Elm to 2nd at the same time.
;; Let's find out who that is:
(def ent-id
  (->> (d/tx-range conn {:start tx-id :end (inc tx-id)})
       first
       :data
       (filter #(= (:v %) "Elm"))
       first
       :e))

(d/pull db '[*] ent-id)