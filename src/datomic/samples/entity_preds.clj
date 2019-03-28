;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns datomic.samples.entity-preds
  (:require [datomic.client.api :as d]))

(defn scores-are-ordered?
  [db eid]
  (let [m (d/pull db [:score/low :score/high] eid)]
    (<= (:score/low m) (:score/high m))))