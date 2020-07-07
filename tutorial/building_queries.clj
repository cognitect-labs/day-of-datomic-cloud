;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local :system "day-of-datomic-cloud"}))
(d/create-database client {:db-name "building-queries"})
(def conn (d/connect client {:db-name "building-queries"}))

(d/transact conn {:tx-data [{:db/ident :user/firstName
                             :db/valueType :db.type/string
                             :db/cardinality :db.cardinality/one}
                            {:db/ident :user/lastName
                             :db/valueType :db.type/string
                             :db/cardinality :db.cardinality/one}]})

;; some St*rts
(d/transact conn {:tx-data [{:user/firstName "Stewart"
                             :user/lastName "Brand"}
                            {:user/firstName "John"
                             :user/lastName "Stewart"}
                            {:user/firstName "Stuart"
                             :user/lastName "Smalley"}
                            {:user/firstName "Stuart"
                             :user/lastName "Halloway"}]})

;; find all the Stewart first names
(d/q '[:find (pull ?e [*])
       :in $ ?name
       :where [?e :user/firstName ?name]]
     db
     "Stewart")

;; find all the Stewart or Stuart first names
(d/q '[:find (pull ?e [*])
       :in $ [?name ...]
       :where [?e :user/firstName ?name]]
     db
     ["Stewart" "Stuart"])

;; find all the Stewart/Stuart as either first name or last name
(d/q '[:find (pull ?e [*])
       :in $ [?name ...] [?attr ...]
       :where [?e ?attr ?name]]
     db
     ["Stewart" "Stuart"]
     [:user/firstName :user/lastName])

;; find only the Smalley Stuarts
(d/q '[:find ?e
       :in $ ?fname ?lname
       :where [?e :user/firstName ?fname]
       [?e :user/lastName ?lname]]
     db
     "Stuart"
     "Smalley")

;; same query as above, but with map form
(d/q {:query '{:find [?e]
               :in [$ ?fname ?lname]
               :where [[?e :user/firstName ?fname]
                       [?e :user/lastName ?lname]]}
      :args [db "Stuart" "Smalley"]})