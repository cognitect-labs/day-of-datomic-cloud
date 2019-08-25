;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; The examples below parallel http://docs.datomic.com/cloud/query/query-executing.html
;; sample data at https://github.com/Datomic/mbrainz-importer

(require '[datomic.client.api :as d])

(def client-cfg (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def conn (d/connect client {:db-name "mbrainz-1968-1973"}))
(def db (d/db conn))

(def query '[:find ?e
             :in $ ?name
             :where [?e :artist/name ?name]])
(d/q query db "The Beatles")
(d/q query db "The Who")

;; not an identical query, ?artist-name instead of ?name
(def query '[:find ?e
             :in $ ?artist-name
             :where [?e :artist/name ?artist-name]])
