;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local :system "datomic-samples"}))
(def conn (d/connect client {:db-name "friends"}))

(def db (d/db conn))

;; get the entity id for anne
(def anne-id
  (ffirst (d/q '[:find ?e :where [?e :person/name "anne"]] db)))

;; use pull to traverse the graph from anne through recursion:
;; a depth of 1
(d/pull db '[[:person/name :as :name] {[:person/friend :as :pals] 1}] anne-id)

;; a depth of 2
(d/pull db '[[:person/name :as :name] {[:person/friend :as :pals] 2}] anne-id)

;; expand all nodes reachable from anne, but don't apply the pull
;; pattern to visited nodes [meaning of ... ]
(d/pull db '[:person/name {:person/friend ...}] anne-id)

;; we can also traverse the graph in reverse (reverse ref in pull pattern)
(d/pull db '[:person/name {[:person/_friend :as :pals] 1}] anne-id)
(d/pull db '[:person/name {[:person/_friend :as :pals] ...}] anne-id)