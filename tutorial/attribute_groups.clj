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

(repl/transact-all conn (repl/resource "day-of-datomic-cloud/social-news.edn"))
(def db (d/db conn))

;; find all attributes in the story namespace
(flatten (d/q '[:find ?e
                :in $
                :where
                [?e :db/valueType]
                [?e :db/ident ?a]
                [(namespace ?a) ?ns]
                [(= ?ns "story")]]
              db))

;; create a reusable rule
(def rules
  '[[[attr-in-namespace ?e ?ns2]
     [?e :db/ident ?a]
     [?e :db/valueType]
     [(namespace ?a) ?ns1]
     [(= ?ns1 ?ns2)]]])

;; find all attributes in story namespace, using the rule
(flatten (d/q '[:find ?e
                :in $ %
                :where
                (attr-in-namespace ?e "story")]
              db rules))

;; find all entities possessing *any* story attribute
(flatten (d/q '[:find ?e
                :in $ %
                :where
                (attr-in-namespace ?a "story")
                [?e ?a]]
              db rules))

(repl/delete-scratch-dbs "config.edn")

