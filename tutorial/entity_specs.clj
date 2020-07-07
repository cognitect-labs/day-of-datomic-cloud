;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;  NOTE: You must run an Ion deploy of the day-of-datomic-cloud project to
;  your Datomic system prior to running these examples.

(require
  '[datomic.client.api :as d]
  '[datomic.samples.repl :as repl])

(def client (d/client {:server-type :dev-local :system "day-of-datomic-cloud"}))
(d/create-database client {:db-name "entity-specs"})
(def conn (d/connect client {:db-name "entity-specs"}))

(def schema [{:db/ident :score/low
              :db/valueType :db.type/long,
              :db/cardinality :db.cardinality/one}
             {:db/ident :score/high,
              :db/valueType :db.type/long,
              :db/cardinality :db.cardinality/one}
             {:db/ident :team/score
              :db.entity/attrs [:score/low :score/high]
              :db.entity/preds 'datomic.samples.entity-preds/scores-are-ordered?}])
(d/transact conn {:tx-data schema})

;; valid scores
(d/transact conn {:tx-data [{:score/low 10
                             :score/high 75
                             :db/ensure :team/score}]})

;; not valid - must have low and high score
(-> (d/transact conn {:tx-data [{:db/id "my-score"
                                 :score/low 10
                                 :db/ensure :team/score}]})
    repl/thrown-data)

;; not valid - scores must match predicate
(-> (d/transact conn {:tx-data [{:db/id "my-score"
                                 :score/low 100
                                 :score/high 20
                                 :db/ensure :team/score}]})
    repl/thrown-data)