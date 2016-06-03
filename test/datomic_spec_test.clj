(ns datomic-spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [datomic-spec :refer :all]))

(deftest test-query-spec
  (testing "basic query"
    (is (not (s/valid? :datomic-spec/query '{})))
    (is (s/valid? :datomic-spec/query '{:find [?e]})))
  (testing "with with-clause"
    (is (not (s/valid? :datomic-spec/query '{:find [?e] :with :not-a-list})))
    (is (s/valid? :datomic-spec/query '{:find [?e] :with [?monster]})))
  (testing "with inputs"
    (is (not (s/valid? :datomic-spec/query '{:find [?e] :in :not-a-list})))
    (is (s/valid? :datomic-spec/query '{:find [?e] :in [$]})))
  (testing "with where-clauses"
    (is (not (s/valid? :datomic-spec/query '{:find [?e] :where :not-a-list})))
    (is (s/valid? :datomic-spec/query '{:find [?e] :where [[?e :age 42]]})))
  (testing "in list form"
    (is (s/valid? :datomic-spec/query
                  '[:find ?e :with ?monster :in $ :where [?e :age 42]]))))

