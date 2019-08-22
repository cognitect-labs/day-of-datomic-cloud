;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; sample data at https://github.com/Datomic/mbrainz-importer

(require '[datomic.client.api :as d])

(def cfg (read-string (slurp "config.edn")))
(def client (d/client cfg))
(def db-name "mbrainz-1968-1973")
(def conn (d/connect client {:db-name db-name}))
(d/db-stats (d/db conn))
;; {:datoms 1101457}

(def rules '[[(created-at ?eid ?created-at)
              [?eid :artist/gid _ ?tx]
              [?tx :db/txInstant ?created-at]]
             [(modified-at ?eid ?modified-at)
              [?eid _ _ ?tx]
              [?tx :db/txInstant ?modified-at]]])

(dotimes [_ 10]
  (time (d/q '[:find (count ?modified-at) ?created-at
               :in $ % ?name
               :where
               (created-at ?e ?created-at)
               (modified-at ?e ?modified-at)
               [?e :artist/name ?name]]
             (d/db conn)
             rules
             "John Lennon")))

;"Elapsed time: 141.830006 msecs"
;"Elapsed time: 106.182582 msecs"
;"Elapsed time: 153.978252 msecs"
;"Elapsed time: 152.049875 msecs"
;"Elapsed time: 101.1425 msecs"
;"Elapsed time: 111.393335 msecs"
;"Elapsed time: 97.178747 msecs"
;"Elapsed time: 115.192522 msecs"
;"Elapsed time: 107.059504 msecs"
;"Elapsed time: 100.019785 msecs"


; Binding the ?e input to the rule prior to invoking it
; results in a ~5x speed up:

(dotimes [_ 10]
  (time (d/q '[:find (count ?modified-at) ?created-at
               :in $ % ?name
               :where
               [?e :artist/name ?name]
               (created-at ?e ?created-at)
               (modified-at ?e ?modified-at)]
             (d/db conn)
             rules
             "John Lennon")))

;"Elapsed time: 65.252007 msecs"
;"Elapsed time: 32.437732 msecs"
;"Elapsed time: 26.300127 msecs"
;"Elapsed time: 33.456768 msecs"
;"Elapsed time: 32.137735 msecs"
;"Elapsed time: 25.241188 msecs"
;"Elapsed time: 32.067527 msecs"
;"Elapsed time: 26.093559 msecs"
;"Elapsed time: 25.86028 msecs"
;"Elapsed time: 27.734526 msecs"

; Specifying that ?eid must be bound in the rule head
; provides the same performance advantage, independent
; of whether the rule or the e-binding clause occurs
; first in the query:

(def rules-req-bindings '[[(created-at [?eid] ?created-at)
                           [?eid :artist/gid _ ?tx]
                           [?tx :db/txInstant ?created-at]]
                          [(modified-at [?eid] ?modified-at)
                           [?eid _ _ ?tx]
                           [?tx :db/txInstant ?modified-at]]
                          [(artist-name ?eid ?name)]])

(dotimes [_ 10]
  (time (d/q '[:find (count ?modified-at) ?created-at
               :in $ % ?name
               :where
               (created-at ?e ?created-at)
               (modified-at ?e ?modified-at)
               [?e :artist/name ?name]]
             (d/db conn)
             rules-req-bindings
             "John Lennon")))

;"Elapsed time: 66.176336 msecs"
;"Elapsed time: 33.042925 msecs"
;"Elapsed time: 27.7784 msecs"
;"Elapsed time: 38.535327 msecs"
;"Elapsed time: 45.482539 msecs"
;"Elapsed time: 39.341726 msecs"
;"Elapsed time: 31.054759 msecs"
;"Elapsed time: 33.234804 msecs"
;"Elapsed time: 31.42365 msecs"
;"Elapsed time: 33.532647 msecs"
