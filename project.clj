(defproject day-of-datomic-cloud "0.1.0-SNAPSHOT"
  :description "Day of Datomic Cloud"
  :source-paths ["src" "tutorial" "doc-examples"]
  :resource-paths ["resources"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.generators "0.1.2"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [com.datomic/client-cloud "0.8.71"]
                 [seesaw/seesaw "1.4.5"]])

