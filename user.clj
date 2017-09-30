(require
 '[clojure.pprint :as pp]
 '[datomic.dodc.repl-ui :as ui :refer (font label! table! tree!)]
 '[datomic.client.api.alpha :as d]
 '[seesaw.core :as ss]
 '[seesaw.table :as table]
 '[seesaw.tree :as tree])
(set! *print-length* 25)
