;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl]
         '[datomic.samples.schema :as schema])

(def client (d/client {:server-type :dev-local :system "day-of-datomic-cloud"}))
(d/create-database client {:db-name "migration"})
(def conn (d/connect client {:db-name "migration"}))

(def schema-map (-> (repl/resource "day-of-datomic-cloud/schema.edn")
                    slurp read-string))

(schema/has-attribute? (d/db conn) :story/title)

;; install the :day-of-datomic/provenance schema
(schema/ensure-schemas conn :day-of-datomic/schema schema-map :day-of-datomic/provenance)

(def db (d/db conn))

;; now we have the provenance attributes
(schema/has-attribute? db :source/user)

;; and we have the social-news attributes, because provenance
;; depended on them
(schema/has-attribute? db :story/title)