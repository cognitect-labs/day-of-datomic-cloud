;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])
(import 'java.util.UUID)

(def client (d/client {:server-type :dev-local :system "day-of-datomic-cloud"}))
(d/create-database client {:db-name "query-for-ids"})
(def conn (d/connect client {:db-name "query-for-ids"}))

(set! *print-length* 10)

(d/transact conn {:tx-data
                  [{:db/ident :color/green}
                   {:db/ident :color/red}
                   {:db/ident :color/blue}
                   {:db/ident :item/color
                    :db/cardinality :db.cardinality/one
                    :db/valueType :db.type/ref}]})

;; create 100,000 items, colored at random (takes a few seconds)
(d/transact
  conn
  {:tx-data (repeatedly 100000 (fn [] [:db/add (str (UUID/randomUUID)) :item/color
                                       (rand-nth [:color/green :color/red :color/blue])]))})

(def db (d/db conn))

;; query directly for id: fast
(dotimes [_ 10]
  (time (d/q '[:find (count ?item)
               :where [?item :item/color :color/green]]
             db)))

;; add unnecessary indirection to ident: slower
(dotimes [_ 10]
  (time (d/q '[:find (count ?item)
               :where [?item :item/color ?color]
               [?color :db/ident :color/green]]
             db)))