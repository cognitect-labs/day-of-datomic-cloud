;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api.alpha :as d]
         '[datomic.samples.repl :as repl]
         '[clojure.pprint :as pp])

(def conn (repl/scratch-db-conn "config.edn"))
(repl/transact-all conn (repl/resource "day-of-datomic-cloud/social-news.edn"))
(repl/transact-all conn (repl/resource "day-of-datomic-cloud/provenance.edn"))

(def db (d/db conn))

;; basis-t is t of most recent transaction 
(def basis-t (:t db))

;; find the most recent transaction tx
(def latest-tx
  (-> (d/tx-range conn {:start basis-t :end nil})
      first :tx-data first :t))

;; facts about the most recent transaction
(d/pull db '[*] latest-tx)

;; how many datoms in most recent transaction?
(-> (d/tx-range conn {:start latest-tx :end (inc latest-tx)})
    seq first :tx-data count)

(repl/delete-scratch-dbs "config.edn")