(ns datomic.dodc.repl-ui-fx
  (:require
   [clojure.spec.alpha :as s]
   [datomic.dodc.repl-ui.specs :as specs]
   [fn-fx.fx-dom :as dom]
   [fn-fx.diff :refer [defui]]
   [fn-fx.controls :as ui]))

(defmulti render-data (fn [{:keys [spec]}]
                        spec))

(defmethod render-data :default
  [{:keys [spec]}]
  (ui/label :text (str "No Renderer for " spec)))

(defui Stage
  (render [this {:keys [title] :as state}]
          (ui/stage
           :title title
           :shown true
           :min-width 600
           :min-height 600
           :scene (ui/scene
                   :root (ui/stack-pane
                          :children [(render-data state)])))))

(def spec-ordering
  (atom
   [::specs/table
    ::specs/schema-by-ns
    ::specs/rectangle]))

(defn spec-for [x]
  (or (some #(when (s/vlaid? % x)
               %) @spec-ordering)
      ::unknown))

;;; RENDERERS

(defmethod render-data ::specs/table
  [{:keys [data]}]
  (let [[column-defs rows] data]
    (ui/table-view
     ;:min-width 600
     ;:min-height 600
     :columns (for [column column-defs]
                              (ui/table-column
                               :text (pr-str column)
                               :cell-value-factory (ui/map-value-factory :key column)))
                   :items rows)))


;;;

(defn render [data-state data]
  (let [spec (spec-for data)]
    (println "SPEC " spec)
    (send data-state assoc :spec spec :data data :title (str "REPL Eval: " spec))))

(defn render-and-print
  "REPL printer that will render eval results into a JavaFX window,
if they match a spec in renderers-ref. Use with e.g.

   (main/repl :print (ui/render-and-print))
"
  []
  (let [data-state (agent {:title "REPL Eval"
                           :spec nil
                           :data nil})
        handler-fn (fn [event] nil) ;; No UI events for now
        ui-state (agent (dom/app (stage @data-state) handler-fn))]
    (add-watch data-state :ui (fn [_ _ _ _]
                                (send ui-state
                                      (fn [old-ui]
                                        (dom/update-app old-ui (stage @data-state))))))
    (fn [x]
      (render data-state x)
      (prn x))))


(comment

  (let [f (render-and-print)]
    (dotimes [x 10]
      (f [[:x :y] [{:x 1 :y 2} {:x x :y 3}]])
      (Thread/sleep 1000))
    (f "Hey"))

  (spec-for [[:x :y] [{:x 1 :y 2} {:x 2 :y 3}]])



  )



