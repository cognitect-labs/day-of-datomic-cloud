(ns datomic.dodc.repl-ui
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.spec.alpha :as s]
   [datomic.client.api.alpha :as d]
   [datomic.dodc.repl-ui.specs :as specs]
   [seesaw.core :as ss]
   [seesaw.font :as font]
   [seesaw.tree :as tree])
  (:import
   [java.io File]
   [javax.swing JTable]))

(set! *warn-on-reflection* true)

(defonce frame
  (delay (-> (ss/frame :title "Day of Datomic")
             ss/pack! ss/show!)))

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

(def renderers-ref
  (atom
   [[::specs/table
     (fn [[titles content]]
       (ss/scrollable (table* :model [:columns titles
                                      :rows (take 1000 content)]
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
                                      :rows (take 1000 data)]
                              :font (font :size (font-size))
                              :show-grid? true?)))]]))

(defn render
  [x]
  (reduce
   (fn [_ [spec f]]
     (when (s/valid? spec x)
       (ss/config! @frame :content (f x))
       (reduced spec)))
   nil
   @renderers-ref))

(defn render-and-print
  "REPL printer that will render eval results into a Swing window,
if they match a spec in renderers-ref. Use with e.g.

   (main/repl :print ui/render-and-print)
"
  [x]
  (render x)
  (prn x))

