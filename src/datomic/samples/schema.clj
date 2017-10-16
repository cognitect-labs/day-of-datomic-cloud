(ns datomic.samples.schema
  (:require [datomic.client.api :as d]))

(defn cardinality
  "Returns the cardinality (:db.cardinality/one or
   :db.cardinality/many) of the attribute"
  [db attr]
  (-> (d/pull db
              {:selector '[:db/cardinality]
               :eid attr})
      (get-in [:db/cardinality :db/ident])))

(defn has-attribute?
  "Does database have an attribute named attr-name?"
  [db attr-name]
  (-> (d/pull db
              {:selector '[:db/valueType]
               :eid attr-name})
      seq
      boolean))

(defn has-schema?
  "Does database have a schema named schema-name installed?
   Uses schema-attr (an attribute of transactions!) to track
   which schema names are installed."
  [conn db schema-attr schema-name]
  (and (has-attribute? db schema-attr)
       (-> (d/q {:query '[:find ?e
                          :in $ ?sa ?sn
                          :where [?e ?sa ?sn]]
                 :args [db schema-attr schema-name]})
           seq
           boolean)))

(defn- ensure-schema-attribute
  "Ensure that schema-attr, a keyword-valued attribute used
   as a value on transactions to track named schemas, is
   installed in database."
  [conn schema-attr]
  (when-not (has-attribute? (d/db conn) schema-attr)
    (d/transact conn {:tx-data [{:db/ident schema-attr
                                 :db/valueType :db.type/keyword
                                 :db/cardinality :db.cardinality/one
                                 :db/doc "Name of schema installed by this transaction"}]})))

(defn ensure-schemas
  "Ensure that schemas are installed.

      schema-attr   a keyword valued attribute of a transaction,
                    naming the schema
      schema-map    a map from schema names to schema installation
                    maps. A schema installation map contains two
                    keys: :txes is the data to install, and :requires
                    is a list of other schema names that must also
                    be installed
      schema-names  the names of schemas to install"
  [conn schema-attr schema-map & schema-names]
  (ensure-schema-attribute conn schema-attr)
  (doseq [schema-name schema-names]
    (when-not (has-schema? conn (d/db conn) schema-attr schema-name)
      (let [{:keys [requires txes]} (get schema-map schema-name)]
        (apply ensure-schemas conn schema-attr schema-map requires)
        (if txes
          (doseq [tx txes]
            ;; hrm, could mark the last tx specially
            (d/transact conn {:tx-data (cons {:db/id "datomic.tx"
                                              schema-attr schema-name}
                                             tx)}))
          (throw (ex-info (str "No data provided for schema" schema-name)
                          {:schema/missing schema-name})))))))


