(ns datomic.dodc.ui
  (:require [seesaw.core :as ss]))

(defonce frame
  (delay (-> (ss/frame :title "Day of Datomic")
             ss/pack! ss/show!)))

(defn display!
  [content]
  (ss/config! @frame :content content)
  content)

(defn table!
  [titles content]
  (ss/config! @frame :content (ss/table :model [:columns titles
                                                :rows content]
                                        :show-grid? true))
  content)

(defn label!
  [label]
  (ss/config! @frame :content (ss/label label)))
