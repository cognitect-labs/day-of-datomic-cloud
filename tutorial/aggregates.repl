;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])
(import '(java.util UUID))

(def client-cfg (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "aggregates-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(def schema
  [{:db/ident :object/name
    :db/doc "Name of a Solar System object."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :object/meanRadius
    :db/doc "Mean radius of an object."
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one}
   {:db/ident :data/source
    :db/doc "Source of the data in a transaction."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(d/transact conn {:tx-data schema})

(def data
  [{:db/doc "Solar system objects bigger than Pluto."}
   {:data/source "http://en.wikipedia.org/wiki/List_of_Solar_System_objects_by_size"}
   {:object/name "Sun"
    :object/meanRadius 696000.0}
   {:object/name "Jupiter"
    :object/meanRadius 69911.0}
   {:object/name "Saturn"
    :object/meanRadius 58232.0}
   {:object/name "Uranus"
    :object/meanRadius 25362.0}
   {:object/name "Neptune"
    :object/meanRadius 24622.0}
   {:object/name "Earth"
    :object/meanRadius 6371.0}
   {:object/name "Venus"
    :object/meanRadius 6051.8}
   {:object/name "Mars"
    :object/meanRadius 3390.0}
   {:object/name "Ganymede"
    :object/meanRadius 2631.2}
   {:object/name "Titan"
    :object/meanRadius 2576.0}
   {:object/name "Mercury"
    :object/meanRadius 2439.7}
   {:object/name "Callisto"
    :object/meanRadius 2410.3}
   {:object/name "Io"
    :object/meanRadius 1821.5}
   {:object/name "Moon"
    :object/meanRadius 1737.1}
   {:object/name "Europa"
    :object/meanRadius 1561.0}
   {:object/name "Triton"
    :object/meanRadius 1353.4}
   {:object/name "Eris"
    :object/meanRadius 1163.0}])

(d/transact conn {:tx-data data})

(def db (d/db conn))

;; how many objects are there?
(d/q '[:find (count ?e)
       :where [?e :object/name ?n]]
     db)

;; largest radius?
(d/q '[:find (max ?radius)
       :where [_ :object/meanRadius ?radius]]
     db)

;; smallest radius
(d/q '[:find (min ?radius)
       :where [_ :object/meanRadius ?radius]]
     db)

;; average radius
(d/q '[:find (avg ?radius)
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; median radius
(d/q '[:find (median ?radius)
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; stddev
(d/q '[:find (stddev ?radius)
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; random solar system object
(d/q '[:find (rand ?name)
       :where [?e :object/name ?name]]
     db)

;; smallest 3
(d/q '[:find (min 3 ?radius)
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; largest 3
(d/q '[:find (max 3 ?radius)
       :with ?e
       :where [?e :object/meanRadius ?radius]]
     db)

;; 5 random (duplicates possible)
(d/q '[:find (rand 5 ?name)
       :with ?e
       :where [?e :object/name ?name]]
     db)

;; choose 5, no duplicates
(d/q '[:find (sample 5 ?name)
       :with ?e
       :where [?e :object/name ?name]]
     db)

;; what is the average length of a
;; schema name?
(d/q '[:find (avg ?length)
       :with ?e
       :where
       [?e :db/ident ?ident]
       [(name ?ident) ?name]
       [(count ?name) ?length]]
     db)

;; how many attributes and value types does this
;; schema use?
(d/q '[:find  (count ?a) (count-distinct ?vt)
       :where
       [?a :db/ident ?ident]
       [?a :db/valueType ?vt]]
     db)

(d/delete-database client {:db-name db-name})
