;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(require '[datomic.client.api.alpha :as d]
         '[datomic.samples.repl :as repl])

(def conn (repl/scratch-db-conn "config.edn"))

;; define a schema of person entities where each person has a name and friends.
;; -- note that the friends are intended to be person entities themselves.
(def schema-tx
  [{:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/friend
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}])

(d/transact conn {:tx-data schema-tx})

;; transact some people who have some friends.
(let [people-tx
      [{:db/id "anne-id"
        :person/name "anne"
        :person/friend #{"bob-id" "james-id"}}
       {:db/id "bob-id"
        :person/name "bob"
        :person/friend #{"anne-id" "lucille-id"}}
       {:db/id "james-id"
        :person/name "james"
        :person/friend #{"anne-id" "lucille-id"}}
       {:db/id "lucille-id"
        :person/name "lucille"
        :person/friend #{"bob-id"}}]]
  (d/transact conn {:tx-data people-tx}))

;; get the entity id for anne
(def anne-id
  (ffirst (d/q '[:find ?e :where [?e :person/name "anne"]] (d/db conn))))

;; use pull to traverse the graph from anne through recursion:
;; a depth of 1
(d/pull (d/db conn) '[:person/name {:person/friend 1}] anne-id)

;; a depth of 2
(d/pull (d/db conn) '[:person/name {:person/friend 2}] anne-id)

;; expand all nodes reachable from anne, but don't apply the pull
;; pattern to visited nodes [meaning of ... ]
(d/pull (d/db conn) '[:person/name {:person/friend ...}] anne-id)

;; we can also traverse the graph in reverse (reverse ref in pull pattern)
(d/pull (d/db conn) '[:person/name {:person/_friend 1}] anne-id)
(d/pull (d/db conn) '[:person/name {:person/_friend ...}] anne-id)

(repl/delete-scratch-db conn "config.edn")