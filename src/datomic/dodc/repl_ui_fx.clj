(ns datomic.dodc.repl-ui-fx
  (:require
   [clojure.spec.alpha :as s]
   [datomic.dodc.repl-ui.specs :as specs]
   [fn-fx.fx-dom :as dom]
   [fn-fx.controls :as ui]))

(defn stage
  [title]
  (ui/stage :title title
            :shown true
            :min-width 300
            :min-height 300))

(defonce dom-ref
  (delay
   (atom (dom/app (stage "REPL eval")))))

;; TODO: how to get discover the options supported by the various controls?
(def renderers-ref
  (atom
   [[::specs/table identity]
    [::specs/schema-by-ns identity]
    [::specs/rectangle identity]]))

(defn render
  [x]
  (reduce
   (fn [_ [spec f]]
     (when (s/valid? spec x)
       (swap! @dom-ref dom/update-app (stage (str spec)))
       (reduced spec)))
   nil
   @renderers-ref))

(defn render-and-print
  "REPL printer that will render eval results into a JavaFX window,
if they match a spec in renderers-ref. Use with e.g.

   (main/repl :print ui/render-and-print)
"
  [x]
  (render x)
  (prn x))
