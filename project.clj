(defproject com.theinternate/spec.datomic "0.1.0-SNAPSHOT"
  :description "Specs for Datomic's query data"
  :url "https://github.com/nwjsmith/spec.datomic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha13" :scope "provided"]]
  :profiles {:ci
             {:plugins [[test2junit  "1.2.2"]]
              :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS")
                                          "target/test2junit")}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
