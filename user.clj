(require
 '[clojure.main :as main]
 '[clojure.pprint :as pp]
 '[clojure.spec.alpha :as s]
 '[datomic.dodc.repl-ui :as ui :refer (font label! table! tree!)]
 '[datomic.dodc.repl-ui.specs :as ui-specs]
 '[datomic.client.api.alpha :as d]
 '[seesaw.core :as ss]
 '[seesaw.table :as table]
 '[seesaw.tree :as tree])
(set! *print-length* 25)

