;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local
                       :system "datomic-samples"}))
(def conn (d/connect client {:db-name "mbrainz-subset"}))

(d/db-stats (d/db conn))
;; {:datoms 800958}

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

;"Elapsed time: 795.416999 msecs"
;"Elapsed time: 509.75383 msecs"
;"Elapsed time: 552.461563 msecs"
;"Elapsed time: 521.160344 msecs"
;"Elapsed time: 492.732056 msecs"
;"Elapsed time: 481.658969 msecs"
;"Elapsed time: 518.658198 msecs"
;"Elapsed time: 458.666145 msecs"
;"Elapsed time: 469.069394 msecs"
;"Elapsed time: 493.37032 msecs"

; Binding the ?e input to the rule prior to invoking it
; results in a ~100x speed up:

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

;"Elapsed time: 7.271642 msecs"
;"Elapsed time: 4.714193 msecs"
;"Elapsed time: 4.201005 msecs"
;"Elapsed time: 4.362768 msecs"
;"Elapsed time: 4.217789 msecs"
;"Elapsed time: 4.19738 msecs"
;"Elapsed time: 4.170306 msecs"
;"Elapsed time: 4.106451 msecs"
;"Elapsed time: 4.383799 msecs"
;"Elapsed time: 4.013269 msecs"

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

;"Elapsed time: 5.644502 msecs"
;"Elapsed time: 3.928514 msecs"
;"Elapsed time: 4.252462 msecs"
;"Elapsed time: 3.934261 msecs"
;"Elapsed time: 3.884296 msecs"
;"Elapsed time: 3.689085 msecs"
;"Elapsed time: 3.622701 msecs"
;"Elapsed time: 3.824715 msecs"
;"Elapsed time: 4.050259 msecs"
;"Elapsed time: 3.88406 msecs"