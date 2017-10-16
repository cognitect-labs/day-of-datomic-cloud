(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl]
         '[clojure.data.csv :as csv]
         '[clojure.edn :as edn])

(def conn (repl/scratch-db-conn "config.edn"))

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

(repl/delete-scratch-db conn "config.edn")