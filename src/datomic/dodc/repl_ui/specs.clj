(ns datomic.dodc.repl-ui.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::attribute (s/keys ::req [:db/id :db/ident :db/valueType :db/cardinality]))
(s/def ::schema-by-ns (s/map-of string? (s/coll-of ::attribute)))

(s/def ::table (s/tuple (s/coll-of keyword?) (s/coll-of (s/map-of keyword? any?))))

(s/def ::rectangle (s/and (s/coll-of #(instance? clojure.lang.Indexed %))
                          (fn [colls]
                            (apply = (map count colls)))))
