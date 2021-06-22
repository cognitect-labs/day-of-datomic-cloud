;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; sample data at https://github.com/Datomic/mbrainz-importer

(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local :system "examples"}))
(def conn (d/connect client {:db-name "mbrainz-1968-1973"}))
(def db (d/db conn))

;; This query will find only artists that have both a name and a start year:
(d/q '[:find ?name ?year
       :where
       [?e :artist/name ?name]
       [?e :artist/startYear ?year]]
     db)

;; What if you want all artists, regardless of whether they have a start year?
;; In SQL you might use an outer join.
;; In Datomic, you can query for the artists, and pull the details.
;; This separation allows you to reuse 'find' and 'detail' logic independently.
(def find-expr '[:find (pull ?e details-expr)
                 :in $ details-expr
                 :where [?e :artist/name]])
(def details-expr [:artist/name :artist/startYear])
(d/q find-expr db details-expr)

;; Or, if you really like rectangular SQL-ish results, you can use the get-else
;; query function to handle optional columns, returning a default value when no
;; value is present:
(d/q '[:find ?e ?name ?year
       :where
       [?e :artist/name ?name]
       [(get-else $ ?e :artist/startYear "Unknown") ?year]]
     db)


