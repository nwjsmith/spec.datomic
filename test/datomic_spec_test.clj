(ns datomic-spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [datomic-spec :refer :all]))

(deftest test-conforms-for-map-and-list-queries-are-the-same
  (is (= (s/conform :datomic-spec/query
                    '[:find ?e :with ?monster :in $ :where [?e :age 42]])
         (s/conform :datomic-spec/query
                    '{:find [?e]
                      :with [?monster]
                      :in [$]
                      :where [[?e :age 42]]}))))

;; http://docs.datomic.com/query.html#sec-5-1
(deftest test-basic-query
  (is (= '{:find {:spec [:find-rel [[:variable ?e]]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?e]
                                          [:constant :age]
                                          [:constant 42]]}]]]}}
         (s/conform :datomic-spec/query '[:find ?e :where [?e :age 42]]))))

;; http://docs.datomic.com/query.html#sec-5-2
(deftest test-unification-query
  (is (= '{:find {:spec [:find-rel [[:variable ?e] [:variable ?x]]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?e]
                                          [:constant :age]
                                          [:constant 42]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?e]
                                          [:constant :likes]
                                          [:variable ?x]]}]]]}}
         (s/conform :datomic-spec/query '[:find ?e ?x
                                          :where [?e :age 42]
                                                 [?e :likes ?x]]))))

;; http://docs.datomic.com/query.html#sec-5-3
(deftest test-blanks-query
  (is (= '{:find {:spec [:find-rel [[:variable ?x]]]}
           :where {:clauses [[:expression-clause
                                [:data-pattern
                                  {:pattern [[:blank _]
                                             [:constant :likes]
                                             [:variable ?x]]}]]]}}
         (s/conform :datomic-spec/query '[:find ?x
                                          :where [_ :likes ?x]]))))

;; http://docs.datomic.com/query.html#sec-5-5
(deftest test-inputs-query
  (is (= '{:find {:spec [:find-rel [[:variable ?release-name]]]}
           :in {:inputs [[:src-var $]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:src-var $
                                :pattern [[:blank _]
                                          [:constant :release/name]
                                          [:variable ?release-name]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find ?release-name
                      :in $
                      :where [$ _ :release/name ?release-name]]))))

;; http://docs.datomic.com/query.html#sec-5-7-1
(deftest test-tuple-bindings-query
  (is (= '{:find {:spec [:find-rel [[:variable ?release]]]}
           :in {:inputs [[:src-var $] [:binding
                                       [:bind-tuple
                                        {:tuple [[:variable ?artist-name]
                                                 [:variable ?release-name]]}]]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?artist-name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/artists]
                                          [:variable ?artist]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/name]
                                          [:variable ?release-name]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find ?release
                      :in $ [?artist-name ?release-name]
                      :where [?artist :artist/name ?artist-name]
                             [?release :release/artists ?artist]
                             [?release :release/name ?release-name]]))))

;; http://docs.datomic.com/query.html#sec-5-7-2
(deftest test-collection-bindings-query
  (is (= '{:find {:spec [:find-rel [[:variable ?release-name]]]}
           :in {:inputs [[:src-var $] [:binding
                                       [:bind-coll
                                        {:coll {:variable ?artist-name
                                                :ellipses ...}}]]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?artist-name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/artists]
                                          [:variable ?artist]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/name]
                                          [:variable ?release-name]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find ?release-name
                      :in $ [?artist-name ...]
                      :where [?artist :artist/name ?artist-name]
                             [?release :release/artists ?artist]
                             [?release :release/name ?release-name]]))))

;; http://docs.datomic.com/query.html#sec-5-7-3
(deftest test-relation-bindings-query
  (is (= '{:find {:spec [:find-rel [[:variable ?release]]]}
           :in {:inputs [[:src-var $] [:binding
                                       [:bind-rel
                                        {:find-rel
                                         {:variables
                                          [[:variable ?artist-name]
                                           [:variable ?release-name]]}}]]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?artist-name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/artists]
                                          [:variable ?artist]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/name]
                                          [:variable ?release-name]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find ?release
                      :in $ [[?artist-name ?release-name]]
                      :where [?artist :artist/name ?artist-name]
                             [?release :release/artists ?artist]
                             [?release :release/name ?release-name]]))))

;; http://docs.datomic.com/query.html#sec-5-8
(deftest test-find-spec-rel
  (is (= '{:find {:spec [:find-rel [[:variable ?artist-name]
                                    [:variable ?release-name]]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/name]
                                          [:variable ?release-name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/artists]
                                          [:variable ?artist]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?artist-name]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find ?artist-name ?release-name
                      :where [?release :release/name ?release-name]
                             [?release :release/artists ?artist]
                             [?artist :artist/name ?artist-name]]))))

(deftest test-find-spec-coll
  (is (= '{:find {:spec [:find-coll {:coll {:elem [:variable ?release-name]
                                            :ellipses ...}}]}
           :in {:inputs [[:src-var $] [:variable ?artist-name]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?artist-name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/artists]
                                          [:variable ?artist]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/name]
                                          [:variable ?release-name]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find [?release-name ...]
                      :in $ ?artist-name
                      :where [?artist :artist/name ?artist-name]
                             [?release :release/artists ?artist]
                             [?release :release/name ?release-name]]))))

(deftest test-find-spec-tuple
  (is (= '{:find {:spec [:find-tuple {:tuple [[:variable ?year]
                                              [:variable ?month]
                                              [:variable ?day]]}]}
           :in {:inputs [[:src-var $] [:variable ?name]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/startDay]
                                          [:variable ?day]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/startMonth]
                                          [:variable ?month]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/startYear]
                                          [:variable ?year]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find [?year ?month ?day]
                      :in $ ?name
                      :where [?artist :artist/name ?name]
                             [?artist :artist/startDay ?day]
                             [?artist :artist/startMonth ?month]
                             [?artist :artist/startYear ?year]]))))

(deftest test-find-spec-scalar
  (is (= '{:find {:spec [:find-scalar {:elem [:variable ?year]
                                       :period .}]}
           :in {:inputs [[:src-var $] [:variable ?name]]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]
                                          [:variable ?name]]}]]
                             [:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/startYear]
                                          [:variable ?year]]}]]]}}
         (s/conform :datomic-spec/query
                    '[:find ?year .
                      :in $ ?name
                      :where [?artist :artist/name ?name]
                             [?artist :artist/startYear ?year]]))))

;; http://docs.datomic.com/query.html#sec-5-9
(deftest test-not-clause
  (is (= '{:find {:spec [:find-scalar
                         {:elem [:aggregate
                                 {:fn-call {:fn-name count
                                            :fn-args [[:variable ?eid]]}}]
                          :period .}]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?eid]
                                          [:constant :artist/name]]}]]
                             [:not-clause
                              {:condition
                               {:not not
                                :clauses
                                [[:expression-clause
                                  [:data-pattern
                                   {:pattern [[:variable ?eid]
                                              [:constant :artist/country]
                                              [:constant :country/CA]]}]]]}}]]}}
         (s/conform :datomic-spec/query
                    '[:find (count ?eid) .
                      :where [?eid :artist/name]
                             (not [?eid :artist/country :country/CA])]))))

(deftest test-not-join-clause
  (is (= '{:find {:spec [:find-scalar
                         {:elem [:aggregate
                                 {:fn-call {:fn-name count
                                            :fn-args [[:variable ?artist]]}}]
                          :period .}]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?artist]
                                          [:constant :artist/name]]}]]
                             [:not-join-clause
                              {:condition
                               {:not-join not-join
                                :variables [?artist]
                                :clauses
                                [[:expression-clause
                                  [:data-pattern
                                   {:pattern [[:variable ?release]
                                              [:constant :release/artists]
                                              [:variable ?artist]]}]]
                                 [:expression-clause
                                  [:data-pattern
                                   {:pattern [[:variable ?release]
                                              [:constant :release/year]
                                              [:constant 1970]]}]]]}}]]}}
         (s/conform :datomic-spec/query
                    '[:find (count ?artist) .
                      :where [?artist :artist/name]
                      (not-join [?artist]
                                [?release :release/artists ?artist]
                                [?release :release/year 1970])]))))

;; http://docs.datomic.com/query.html#sec-5-10
(deftest test-or-clause
  (is (= '{:find {:spec [:find-scalar
                         {:elem [:aggregate
                                 {:fn-call {:fn-name count
                                            :fn-args [[:variable ?medium]]}}]
                          :period .}]}
           :where {:clauses [[:or-clause
                              {:condition
                               {:or or
                                :clauses
                                [[:clause
                                  [:expression-clause
                                   [:data-pattern
                                    {:pattern
                                     [[:variable ?medium]
                                      [:constant :medium/format]
                                      [:constant :medium.format/vinyl7]]}]]]
                                 [:clause
                                  [:expression-clause
                                   [:data-pattern
                                    {:pattern
                                     [[:variable ?medium]
                                      [:constant :medium/format]
                                      [:constant :medium.format/vinyl10]]}]]]
                                 [:clause
                                  [:expression-clause
                                   [:data-pattern
                                    {:pattern
                                     [[:variable ?medium]
                                      [:constant :medium/format]
                                      [:constant :medium.format/vinyl12]]}]]]
                                 [:clause
                                  [:expression-clause
                                   [:data-pattern
                                    {:pattern
                                     [[:variable ?medium]
                                      [:constant :medium/format]
                                      [:constant :medium.format/vinyl]]}]]]]}}]]}}
         (s/conform :datomic-spec/query
                    '[:find (count ?medium) .
                      :where (or [?medium :medium/format :medium.format/vinyl7]
                                 [?medium :medium/format :medium.format/vinyl10]
                                 [?medium :medium/format :medium.format/vinyl12]
                                 [?medium :medium/format :medium.format/vinyl])]))))

(deftest test-or-clause-with-and-clause
  (is (= '{:find {:spec [:find-scalar
                         {:elem [:aggregate
                                 {:fn-call {:fn-name count
                                            :fn-args [[:variable ?artist]]}}]
                          :period .}]}
           :where {:clauses [[:or-clause
                              {:condition
                               {:or or
                                :clauses
                                [[:clause
                                  [:expression-clause
                                   [:data-pattern
                                    {:pattern
                                     [[:variable ?artist]
                                      [:constant :artist/type]
                                      [:constant :artist.type/group]]}]]]
                                 [:and-clause
                                  {:condition
                                   {:and and
                                    :clauses
                                    [[:expression-clause
                                      [:data-pattern
                                       {:pattern
                                        [[:variable ?artist]
                                         [:constant :artist/type]
                                         [:constant :artist.type/person]]}]]
                                     [:expression-clause
                                      [:data-pattern
                                       {:pattern
                                        [[:variable ?artist]
                                         [:constant :artist/gender]
                                         [:constant :artist.gender/female]]}]]]}}]]}}]]}}
         (s/conform :datomic-spec/query
                    '[:find (count ?artist) .
                      :where (or [?artist :artist/type :artist.type/group]
                                 (and [?artist :artist/type :artist.type/person]
                                      [?artist :artist/gender :artist.gender/female]))]))))

(deftest test-or-join-clause
  (is (= '{:find {:spec [:find-scalar
                         {:elem [:aggregate
                                 {:fn-call {:fn-name count
                                            :fn-args [[:variable ?release]]}}]
                          :period .}]}
           :where {:clauses [[:expression-clause
                              [:data-pattern
                               {:pattern [[:variable ?release]
                                          [:constant :release/name]]}]]
                             [:or-join-clause
                              {:condition
                               {:or-join or-join
                                :variables [?release]
                                :clauses
                                [[:and-clause
                                  {:condition
                                   {:and and
                                    :clauses
                                    [[:expression-clause
                                      [:data-pattern
                                       {:pattern
                                        [[:variable ?release]
                                         [:constant :release/artists]
                                         [:variable ?artist]]}]]
                                     [:expression-clause
                                      [:data-pattern
                                       {:pattern
                                        [[:variable ?artist]
                                         [:constant :artist/country]
                                         [:constant :country/CA]]}]]]}}]
                                 [:clause
                                  [:expression-clause
                                   [:data-pattern
                                    {:pattern
                                     [[:variable ?release]
                                      [:constant :release/year]
                                      [:constant 1970]]}]]]]}}]]}}
         (s/conform :datomic-spec/query
                    '[:find (count ?release) .
                      :where [?release :release/name]
                      (or-join [?release]
                               (and [?release :release/artists ?artist]
                                    [?artist :artist/country :country/CA])
                               [?release :release/year 1970])]))))