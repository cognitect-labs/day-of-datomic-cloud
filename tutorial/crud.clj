;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d]
         '[clojure.pprint :as pp])

(def client (d/client {:server-type :dev-local :system "day-of-datomic-cloud"}))
(d/create-database client {:db-name "crud"})
(def conn (d/connect client {:db-name "crud"}))

;; attribute schema for :crud/name
(d/transact
  conn
  {:tx-data
   [{:db/ident :crud/name
     :db/valueType :db.type/string
     :db/unique :db.unique/identity
     :db/cardinality :db.cardinality/one}]})

;; create, get point-in-time-value
(def db-after-create
  (-> (d/transact
        conn
        {:tx-data [[:db/add "temp-id" :crud/name "Hello world"]]})
      :db-after))

;; read
(d/pull db-after-create '[*] [:crud/name "Hello world"])

;; update
(-> (d/transact
      conn
      {:tx-data [[:db/add [:crud/name "Hello world"]
                  :db/doc "An entity with only demonstration value"]]})
    :db-after
    (d/pull '[*] [:crud/name "Hello world"]))

;; "delete" adds new information, does not erase old
(def db-after-delete
  (-> (d/transact
        conn
        {:tx-data [[:db/retractEntity [:crud/name "Hello world"]]]})
      :db-after))

;; no present value for deleted entity
(d/pull db-after-delete '[*] [:crud/name "Hello world"])

;; but everything ever said is still there
(def history (d/history db-after-delete))

(->> (d/q '[:find ?e ?a ?v ?tx ?op
            :in $
            :where [?e :crud/name "Hello world"]
            [?e ?a ?v ?tx ?op]]
          history)
     (map #(zipmap [:e :a :v :tx :op] %))
     (sort-by :tx)
     (pp/print-table [:e :a :v :tx :op]))

