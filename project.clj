(defproject datomic-spec "0.1.0-SNAPSHOT"
  :description "Specs for Datomic's query data"
  :url "https://github.com/nwjsmith/datomic-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/test.check "0.9.0"]]
  :profiles {:ci
             {:plugins [[test2junit  "1.2.2"]]
              :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS")
                                          "target/test2junit")}})