;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])
(import 'clojure.lang.ExceptionInfo
        'java.util.UUID)

(def client-cfg (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "scratch-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(def schema [{:db/ident :account/number
              :db/cardinality :db.cardinality/one
              :db/valueType :db.type/string
              :db/unique :db.unique/identity}
             {:db/ident :account/balance
              :db/cardinality :db.cardinality/one
              :db/valueType :db.type/double}])

(d/transact conn {:tx-data schema})

(def initial-data [{:account/number "123"
                    :account/balance 100.00}])

(d/transact conn {:tx-data initial-data})

(defn make-deposit!
  ([conn account-id deposit]
   (make-deposit! conn account-id deposit 1))
  ([conn account-id deposit retries]
   (let [db (d/db conn)
         acc-lookup [:account/number account-id]
         balance (-> (d/pull db '[:account/balance] acc-lookup)
                     :account/balance)]
     (try (d/transact conn {:tx-data [[:db/cas acc-lookup
                                       :account/balance balance
                                       (+ balance deposit)]]})
          (catch ExceptionInfo t
            (if (= (:cognitect.anomalies/category (ex-data t)) :cognitect.anomalies/conflict)
              (if (< retries 5)
                (make-deposit! conn account-id deposit (inc retries))
                (throw (java.lang.IllegalStateException. "Max Retries Exceeded")))
              (throw t)))))))

(make-deposit! conn "123" 20.00)

(let [db (d/db conn)
      deposit 850.00
      acc-lookup [:account/number "123"]
      balance-before (-> (d/pull db '[:account/balance] acc-lookup)
                         :account/balance)
      tx-result (d/transact conn {:tx-data [[:db/cas acc-lookup
                                             :account/balance balance-before
                                             (+ balance-before deposit)]]})
      db-after (:db-after tx-result)
      balance-after (-> (d/pull db-after '[:account/balance] acc-lookup)
                  :account/balance)]
  balance-after)

(def conn (d/connect client {:db-name "mbrainz-1968-1973"}))

(d/q '[:find (pull ?e [:artist/name])
       :where [?e :artist/country :country/JP]]
     (d/db conn))

(d/q '[:find (pull ?e [:artist/name :artist/gender])
       :where [?e :artist/country :country/JP]]
     (d/db conn))

(d/q '[:find ?txInstant
       :where [?a :artist/name "The Rolling Stones" ?tx]
       [?tx :db/txInstant ?txInstant]]
     (d/db conn))

(def tx (ffirst
          (d/q '[:find ?tx
                 :where [?a :artist/name "The Rolling Stones" ?tx]]
               (d/db conn))))

(-> (d/tx-range conn {:start tx :end (inc tx)})
    first
    :data)

(d/delete-database client {:db-name db-name})
