;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
  '[datomic.client.api :as d]
  '[datomic.samples.repl :as repl])

(def conn (repl/scratch-db-conn "config.edn"))

(d/transact conn {:tx-data [{:db/ident :room/letter
                             :db/valueType :db.type/string
                             :db/cardinality :db.cardinality/one}
                            {:db/ident :room/number
                             :db/valueType :db.type/long
                             :db/cardinality :db.cardinality/one}
                            {:db/ident :room/number+letter
                             :db/valueType :db.type/tuple
                             :db/cardinality :db.cardinality/one
                             :db/unique :db.unique/value
                             :db/tupleAttrs [:room/number :room/letter]}]})

(d/transact conn {:tx-data [{:room/number 42 :room/letter "B" :room/number+letter []}]})

(d/q '[:find (pull ?e [*])
       :in $ ?number
       :where [?e :room/number ?number]]
     (d/db conn) 42)

(->
  (d/transact conn {:tx-data [{:room/number 42 :room/letter "B" :room/number+letter []}]})
  repl/thrown-data)