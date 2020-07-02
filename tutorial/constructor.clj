;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;  NOTE: You must run an Ion deploy of the day-of-datomic-cloud project to
;  your Datomic system prior to running these examples.

(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl])
(def client (d/client {:server-type :dev-local :system "day-of-datomic-cloud"}))
(def db-name "constructor")
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(def schema [{:db/ident :user/name,
              :db/valueType :db.type/string,
              :db/cardinality :db.cardinality/one,
              :db.attr/preds 'datomic.samples.attr-preds/user-name?}
             {:db/ident :user/email,
              :db/valueType :db.type/string,
              :db/cardinality :db.cardinality/one,}
             {:db/ident :user/validate
              :db.entity/attrs [:user/name :user/email]}])
(d/transact conn {:tx-data schema})

;; valid user
(d/transact conn {:tx-data [{:user/email "jdoe@example.com"
                             :user/name "John Doe"
                             :db/ensure :user/validate}]})

;; not valid -- no email
(-> (d/transact conn {:tx-data [{:user/name "John Doe"
                                 :db/ensure :user/validate}]})
    repl/thrown-data)

;; not valid -- too long
(-> (d/transact conn {:tx-data [{:user/name "This user name is too long"
                                 :db/ensure :user/validate}]})
    repl/thrown-data)




