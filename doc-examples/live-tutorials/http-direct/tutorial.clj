(ns http-direct.tutorial
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(defn read-edn
  [input-stream]
  (some-> input-stream io/reader (java.io.PushbackReader.) edn/read))

(defn handler
  "Returns a cheerful message in response to a :ping in the body"
  [{:keys [headers body]}]
  (let [type (some-> body read-edn)]
    (if (= type :ping)
      {:status 200
       :headers {"Content-Type" "text/plain"}
       :body "Hey! It's working!"}
      {:status 400
       :headers {}
       :body "Expected a :ping"})))
