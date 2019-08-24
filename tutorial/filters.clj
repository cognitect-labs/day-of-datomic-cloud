;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[clojure.pprint :as pp]
         '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl])
(import '(java.util UUID))

(def client-config (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "scratch-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))


(def txes
  [[{:db/id "item/id"
     :db/ident :item/id
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db.install/_attribute :db.part/db}
    {:db/id "item/description"
     :db/ident :item/description
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id "item/count"
     :db/ident :item/count
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id "tx/error"
     :db/ident :tx/error
     :db/valueType :db.type/boolean
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/txInstant #inst "2012"}]
   [{:db/id "DLC-042"
     :item/id "DLC-042"
     :item/description "Dilitihium Crystals"
     :item/count 100}
    {:db/txInstant #inst "2013-01"}]
   [{:item/id "DLC-042"
     :item/count 250}
    {:db/txInstant #inst "2013-02"}]
   [{:item/id "DLC-042"
     :item/count 50}
    {:db/txInstant #inst "2014-02-28"}]
   [{:item/id "DLC-042"
     :item/count 9999}
    {:db/txInstant #inst "2014-04-01"
     :tx/error true}]
   [{:item/id "DLC-042"
     :item/count 100}
    {:db/txInstant #inst "2014-05-15"}]])

(doseq [tx txes]
  (d/transact conn {:tx-data tx}))

(def db (d/db conn))
(def as-of-eoy-2013 (d/as-of db #inst "2014-01-01"))
(def since-2014 (d/since db #inst "2014-01-01"))
(def history (d/history db))

;; print db as a table
(->> (d/datoms history {:index :eavt})
     seq
     (sort repl/tx-part-e-a-added)
     (repl/datom-table history))

(def error-txes (set (d/q '[:find ?e
                            :where [?e :tx/error]]
                          db)))

(d/pull db '[*] [:item/id "DLC-042"])
; => {:db/id 55301036830621764, :item/id "DLC-042", :item/description "Dilitihium Crystals", :item/count 100}

(d/pull as-of-eoy-2013 '[*] [:item/id "DLC-042"])
; => {:db/id 55301036830621764, :item/id "DLC-042", :item/description "Dilitihium Crystals", :item/count 250}

;; common mistake with since:
;; looking up something by key that is not in the time window
(d/pull since-2014 '[*] [:item/id "DLC-042"])
; => #:db{:id nil}

;; solution: lookup with current db, then use since for the entity
(d/pull since-2014 '[*] (:db/id (d/pull db [:db/id] [:item/id "DLC-042"])))
; => {:db/id 55301036830621764, :item/count 100}

;; more likely: multi-point-in-time join
(d/q '[:find ?count
       :in $ $since ?id
       :where [$ ?e :item/id ?id]
       [$since ?e :item/count ?count]]
     db since-2014 "DLC-042")

;; full history of dilithium crystal assertions
(->> (d/q '[:find ?aname ?v ?inst
            :in $ ?e
            :where [?e ?a ?v ?tx true]
            [?tx :db/txInstant ?inst]
            [?a :db/ident ?aname]]
          history [:item/id "DLC-042"])
     (sort-by #(nth % 2))
     pp/pprint)

;; full history of dilithium crystal counts
(->> (d/q '[:find ?inst ?count
            :in $ ?id
            :where [?id :item/count ?count ?tx true]
            [?tx :db/txInstant ?inst]]
          history [:item/id "DLC-042"])
     (sort-by first)
     pp/pprint)

(d/delete-database client {:db-name db-name})
