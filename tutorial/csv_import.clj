(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl]
         '[clojure.data.csv :as csv]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io])
(import '(java.util UUID))

(def client-config (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "scratch-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

@(def csv (with-open [r (io/reader "data/inventory.csv")]
            (into [] (csv/read-csv r))))

@(def headers (first csv))
@(def rows (subvec csv 1))

;; CSV is weaker than JSON or EDN, can't tell strings from numbers, so
;; make schema expicitly after viewing example values
(def schema [{:db/ident :inv/sku
              :db/cardinality :db.cardinality/one
              :db/valueType :db.type/string
              :db/unique :db.unique/identity}
             {:db/ident :inv/count
              :db/cardinality :db.cardinality/one
              :db/valueType :db.type/long}])

(d/transact conn {:tx-data schema})

;; CSV is weaker than JSON or EDN in positionality and in types,
;; so row converter deals with these:
(defn convert-row
  [[sku val]]
  {:inv/sku sku
   :inv/count (edn/read-string val)})

(map convert-row rows)

(d/transact conn {:tx-data (map convert-row rows)})

@(def db (d/db conn))

(d/q '[:find ?sku ?count
       :where [?e :inv/sku ?sku]
              [?e :inv/count ?count]]
     db)

(d/delete-database client {:db-name db-name})
