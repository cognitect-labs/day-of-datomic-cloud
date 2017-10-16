;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns-unalias 'boot.user 'd)

(require '[datomic.client.api.async :as d])

(require '[datomic.client.api :as d])
(d/q '[:find ?nomen
       :where [_ :artist/name ?name]]
     db)

(require '[datomic.client.api.async :as d]
         '[clojure.core.async :refer (<!!)])

(<!! (d/q {:query '[:find ?nomen
                    :where [_ :artist/name ?name]]
           :args [db]}))

(d/q {:query '[:find (count ?name)
               :where [_ :artist/name ?name]]
      :args [db]
      :timeout 1})
(ex-data *e)
 
