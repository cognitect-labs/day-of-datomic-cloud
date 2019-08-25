;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl])
(import '(java.util UUID))

(def client-cfg (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "scratch-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(repl/transact-all conn (repl/resource "day-of-datomic-cloud/social-news.edn"))
(def db (d/db conn))

;; find the idents of all schema elements in the system
(sort (d/q '[:find ?ident
             :where [_ :db/ident ?ident]]
           db))

;; find just the attributes
(sort (d/q '[:find ?ident
             :where
             [?e :db/ident ?ident]
             [_ :db.install/attribute ?e]]
           db))

;; documentation of a schema element
(d/pull db '[:db/doc] :db.unique/identity)

;; complete details of a schema element
(d/pull db '[*] :user/email)

;; find attributes in the user namespace
(sort (d/q '[:find ?ident
             :where
             [?e :db/ident ?ident]
             [_ :db.install/attribute ?e]
             [(namespace ?ident) ?ns]
             [(= ?ns "user")]]
           db))

;; find all reference attributes
(sort (d/q '[:find ?ident
             :where
             [?e :db/ident ?ident]
             [_ :db.install/attribute ?e]
             [?e :db/valueType :db.type/ref]]
           db))

;; find all attributes that are cardinality-many
(sort (d/q '[:find ?ident
             :where
             [?e :db/ident ?ident]
             [_ :db.install/attribute ?e]
             [?e :db/cardinality :db.cardinality/many]]
           db))

(d/delete-database client {:db-name db-name})
