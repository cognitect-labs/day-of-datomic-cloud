(ns datomic.dodc.repl-ui
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.main :as main]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [datomic.client.api :as d]
   [datomic.dodc.repl-ui.specs :as specs]
   [seesaw.core :as ss]
   [seesaw.font :as font]
   [seesaw.tree :as tree])
  (:import
   [java.io File]
   [javax.swing JTable]))

(set! *warn-on-reflection* true)

(defonce frame
  (delay (-> (ss/frame :title "Day of Datomic"
                       :width 600 :height 400)
             ss/show!)))

(defn display!
  [content]
  (ss/config! @frame :content content)
  nil)

(defn table!
  [titles content & more]
  (ss/config! @frame :content (ss/scrollable (apply ss/table :model [:columns titles
                                                                     :rows content]
                                                    :show-grid? true
                                                    more)))
  nil)

(def font font/font)

(defn tree!
  [branch? children root & more]
  (ss/config! @frame :content (ss/scrollable (apply ss/tree :model (tree/simple-tree-model branch? children root)
                                                    :row-height 28
                                                    :font (font :size 24)
                                                    more)))
  nil)

(defn key-by
  [k]
  (map (fn [m] [(k m) (dissoc m k)])))

(defn schema-by-ns
  [db]
  (->> (eduction
        (map first)
        (d/q '[:find (pull ?e [*
                               {:db/valueType [:db/ident]}
                               {:db/cardinality [:db/ident]}])
               :where [?e :db/cardinality]]
             db))
       (sort-by (comp str :db/ident))
       (group-by (comp str namespace :db/ident))
       (into (sorted-map))))

(defn e->ident-map
  [db]
  (->> (d/q '[:find ?e ?ident
              :where [?e :db/ident ?ident]]
            db)
       (into {})))

(defn schema-tree-level
  [x]
  (cond (keyword? (first x)) :leaf
        (string? (ffirst x)) :top
        (string? (first x)) :ns
        (map? (first x)) :attrs
        (map? x) :attr))

(defn schema-tree-branch?
  [x]
  (not= :leaf (schema-tree-level x)))

(defn schema-tree-children
  [x]
  (case (schema-tree-level x)
        :top x
        :ns (second x)
        :attrs x
        :attr (seq x)))

(defn schema-tree-render
  [r info]
  (let [v (:value info)]
    (ss/config! r
                :text (case (schema-tree-level v)
                            :top ""
                            :ns (first v)
                            :attr (str (:db/ident v))
                            (str v)))))

(defn schema-tree-model
  [x]
  (tree/simple-tree-model schema-tree-branch? schema-tree-children (seq x)))

(defn label!
  [label]
  (ss/config! @frame :content (ss/label label)))

(def font-base-ref
  (atom 24))

(defn font-size
  []
  @font-base-ref)

(defn cell-height
  []
  (* @font-base-ref 1.2))

(defn table*
  [& args]
  (doto ^JTable (apply ss/table args)
        (.setRowHeight (cell-height))))

(defn- to-row
  [ks row]
  (if (map? row)
    (map row ks)
    row))

(defn spreadsheet!
  "Opens a temporary csv file containing ks and rows using
whatever you have bound to 'open some.csv' Each row can be
sequential in the same order as the keys, or a map keyed
by the keys."
  [ks rows]
  (let [dir (io/file "temp")
        _ (.mkdirs dir)
        f (File/createTempFile "repl" ".csv" dir)]
    (with-open [w (io/writer f)]
      (csv/write-csv w [ks])
      (csv/write-csv w (map #(to-row ks %) rows)))
    (sh/sh "open" (.getAbsolutePath f))
    f))


(defn- seesaw-row-ize
  "Workaround seesaw keying on vector instead of indexed"
  [x]
  (cond
   (vector? x) x
   (map? x) x
   (instance? clojure.lang.Indexed x) (mapv #(nth x %) (range (count x)))
   :default x))

(def ^:dynamic *render-length* 1000)

(def renderers-ref
  (atom
   [[::specs/table
     (fn [[titles content]]
       (ss/scrollable (table* :model [:columns titles
                                      :rows (take *render-length* content)]
                              :font (font :size (font-size))
                              :show-grid? true)))]
    [::specs/schema-by-ns
     (fn [data]
       (ss/scrollable (ss/tree :model (schema-tree-model data)
                               :row-height (cell-height)
                               :renderer schema-tree-render
                               :font (font :size (font-size)))))]
    [::specs/rectangle
     (fn [data]
       (ss/scrollable (table* :model [:columns (map str (range (count (first data))))
                                      :rows (into [] (comp (take *render-length*) (map seesaw-row-ize)) data)]
                              :font (font :size (font-size))
                              :show-grid? true?)))]]))

(defn render
  [x]
  (reduce
   (fn [_ [spec f]]
     (when (s/valid? spec x)
       (ss/config! @frame :content (f x) :title (str "Rendering: " spec))
       (reduced spec)))
   nil
   @renderers-ref))

(defn identify
  "Convert datom to a vector with idents in e an a position where possible."
  [datom e->ident]
  [(get e->ident (:e datom) (:e datom))
   (get e->ident (:a datom))
   (:v datom)
   (:tx datom)
   (:added datom)])

(defn last-txes
  "Returns a flat collection of datoms from the last n transactions"
  ([conn] (last-txes conn 1))
  ([conn n]
     (let [db (d/db conn)
           e->ident (e->ident-map db)
           t (max 0 (- (:t db) n))]
       (into
        []
        (comp (map :data) cat (map #(identify % e->ident)))
        (d/tx-range conn {:start (inc t) :end (inc (:t db))})))))

(defn render-and-print
  "REPL printer that will render eval results into a Swing window,
if they match a spec in renderers-ref. Use with e.g.

   (main/repl :print ui/render-and-print)
"
  [x]
  (render x)
  (prn x))


(defn abbrev-ns-name
  [n]
  (str/replace n #"(([^.])[^.]*\.)(?=.)" "$2."))

(defn prompt
  []
  (printf "dotd:%s=> " (abbrev-ns-name (ns-name *ns*))))

(defn -main
  [& _]
  (main/repl :print render-and-print
             :prompt prompt))
