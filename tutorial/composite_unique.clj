;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require
  '[datomic.client.api :as d]
  '[datomic.samples.repl :as repl])

(def conn (repl/scratch-db-conn "config.edn"))
(repl/transact-all conn (repl/resource "day-of-datomic-cloud/course-registration.edn"))

(d/transact conn {:tx-data [{:semester/year 2018
                             :semester/season :fall}
                            {:course/id "BIO-101"}
                            {:student/first "John"
                             :student/last "Doe"
                             :student/email "johndoe@university.edu"}]})

(d/transact conn {:tx-data [{:reg/course [:course/id "BIO-101"]
                             :reg/semester [:semester/year+season [2018 :fall]]
                             :reg/student [:student/email "johndoe@university.edu"]}]})

(d/q '[:find (pull ?e [*
                       {:reg/course [*]
                        :reg/semester [*]
                        :reg/student [*]
                        }])
       :where [?e :reg/course]]
     (d/db conn))

(repl/delete-scratch-db conn "config.edn")
