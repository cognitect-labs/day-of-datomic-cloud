;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.repl
  (:require [clojure.data.generators :as gen]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [datomic.client.api :as d])
  (:import java.util.Random
           (java.util UUID)))

(def resource io/resource)

(defn- delete-all-scratch-dbs
  [cfg-file]
  "Deletes all DBs with the prefix \"scratch-\""
  (let [cfg (read-string (slurp cfg-file))
        client (d/client cfg)
        db-names (d/list-databases client {})]
    (doseq [db-name db-names
            :when (.startsWith db-name "scratch-")]
      (println "Deleting DB: " db-name)
      (d/delete-database client {:db-name db-name}))))

(defn- read-one
  [r]
  (try
    (read r)
    (catch java.lang.RuntimeException e
      (if (= "EOF while reading" (.getMessage e))
        ::EOF
        (throw e)))))

(defn read-all
  "Reads a sequence of top-level objects in file"
  ;; Modified from Clojure Cookbook, L Vanderhart & R. Neufeld
  [src]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader src))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

(defn transact-all
  "Load and run all transactions from f, where f is any
   resource that can be opened by io/reader."
  [conn f]
  (loop [n 0
         [tx & more] (read-all f)]
    (if tx
      (recur (+ n (count (:tx-data (d/transact conn {:tx-data tx}))))
             more)
      {:datoms n})))

(defn transcript
  "Run all forms, printing a transcript as if forms were
   individually entered interactively at the REPL."
  [forms]
  (binding [*ns* *ns*]
    (let [temp (gensym)]
      (println ";; Executing forms in temp namespace: " temp)
      (in-ns temp)
      (clojure.core/use 'clojure.core 'clojure.repl 'clojure.pprint)
      (doseq [f forms]
        (pp/pprint f)
        (print "=> ")
        (pp/pprint (eval f))
        (println))
      (remove-ns temp)
      :done)))

(defmacro should-throw
  "Runs forms, expecting an exception. Prints descriptive message if
   an exception occurred. Throws if an exception did *not* occur."
  [& forms]
  `(try
     ~@forms
     (throw (ex-info "Expected exception" {:forms '~forms}))
     (catch Throwable t#
       (println "Got expected exception:\n\t" (.getMessage t#)))))

(defn modes
  "Returns the set of modes for a collection."
  [coll]
  (->> (frequencies coll)
       (reduce
         (fn [[modes ct] [k v]]
           (cond
             (< v ct)  [modes ct]
             (= v ct)  [(conj modes k) ct]
             (> v ct) [#{k} v]))
         [#{} 2])
       first))

(defn generate-some-comments
  "Generates transaction data for some comments"
  [conn db n]
  (let [story-ids (->> (d/q {:query '[:find ?e
                                      :where [?e :story/url]]
                             :args [db]})
                       (mapv first))
        user-ids (->> (d/q {:query '[:find ?e
                                     :where [?e :user/email]]
                            :args [db]})
                      (mapv first))
        comment-ids (->> (d/q {:query '[:find ?e
                                        :where [?e :comment/author]]
                               :args [db]})
                         (mapv first))
        choose1 (fn [n] (when (seq n) (gen/rand-nth n)))]
    (assert (seq story-ids))
    (assert (seq user-ids))
    (->> (fn []
           (let [comment-id (str (UUID/randomUUID))
                 parent-id (or (choose1 comment-ids) (choose1 story-ids))]
             [[:db/add parent-id :comments comment-id]
              [:db/add comment-id :comment/author (choose1 user-ids)]
              [:db/add comment-id :comment/body "blah"]]))
         (repeatedly n)
         (mapcat identity))))

(defn setup-sample-db-1
  [conn]
  (doseq [schema ["day-of-datomic-cloud/social-news.edn"
                  "day-of-datomic-cloud/provenance.edn"]]
    (->> (io/resource schema)
         (transact-all conn)))
  (let [[[ed]] (seq (d/q {:query '[:find ?e
                                   :where [?e :user/email "editor@example.com"]]
                          :args [(d/db conn)]}))]
    (d/transact conn {:tx-data [[:db/add ed :user/firstName "Edward"]]}))
  (binding [gen/*rnd* (Random. 42)]
    (dotimes [_ 4]
      (d/transact conn {:tx-data (generate-some-comments conn (d/db conn) 5)}))
    conn))

(defn trunc
  "Return a string rep of x, shortened to n chars or less"
  [x n]
  (let [s (str x)]
    (if (<= (count s) n)
      s
      (str (subs s 0 (- n 3)) "..."))))

(def tx-part-e-a-added
  "Sort datoms by tx, then e, then a, then added"
  (reify
    java.util.Comparator
    (compare
      [_ x y]
      (cond
        (< (:tx x) (:tx y)) -1
        (> (:tx x) (:tx y)) 1
        (< (:e x) (:e y)) -1
        (> (:e x) (:e y)) 1
        (< (:a x) (:a y)) -1
        (> (:a x) (:a y)) 1
        (false? (:added x)) -1
        :default 1))))

(defn root-cause
  [^Throwable x]
  (when x
    (let [cause (.getCause x)]
      (if cause
        (recur cause)
        x))))

(defn find-ex-data
  "Returns the first t in the cause chain that has ex-data."
  [^Throwable t]
  (loop [t t]
    (when t
      (if (ex-data t)
        t
        (recur (.getCause t))))))

(defmacro thrown-data
  [& body]
  `(try
     ~@body
     (catch Throwable t#
       (ex-data (find-ex-data t#)))))
