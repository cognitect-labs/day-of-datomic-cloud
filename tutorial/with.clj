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
(repl/transact-all conn (repl/resource "day-of-datomic-cloud/monsters.edn"))

(def db (d/db conn))

;; This will return 4, not 6. Correct and consistent, but not
;; very useful.
(d/q '[:find (sum ?heads)
       :where [_ :monster/heads ?heads]]
     db)

;; returning the base set for the aggregate reveals the problem:
;; sets don't have duplicates
(d/q '[:find ?heads
       :where [_ :monster/heads ?heads]]
     db)

;; the :with clause considers additional variables when forming
;; the basis set for the query result. These additional variables
;; are then removed, leaving a useful bag (not a set!) of values
;; scoped by the :with variables.
(d/q '[:find ?heads
       :with ?monster
       :where [?monster :monster/heads ?heads]]
     db)

;; you will typically want a ":with ?someentity" when computing
;; aggregates, where ?someentity owns the values you are aggregating
;; over.
(d/q '[:find (sum ?heads)
       :with ?monster
       :where [?monster :monster/heads ?heads]]
     db)

(repl/delete-scratch-db conn "config.edn")