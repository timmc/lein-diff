(ns leiningen.t-diff
  (:require [leiningen.diff :as d]
            [clojure.test :refer :all]))

(deftest comparison
  (testing "empty/empty"
    (is (= (d/compare-dep-lists {} {})
           {:common {}
            :added {}
            :removed {}
            :changed ()})))
  (testing "unchanged"
    (is (= (d/compare-dep-lists {"foo" "1"} {"foo" "1"})
           {:common {"foo" "1"}
            :added {}
            :removed {}
            :changed ()})))
  (testing "empty/some"
    (is (= (d/compare-dep-lists {} {"foo/bar" "2.3", "baz" "0"})
           {:common {}
            :added {"foo/bar" "2.3", "baz" "0"}
            :removed {}
            :changed ()})))
  (testing "some/empty"
    (is (= (d/compare-dep-lists {"foo/bar" "2.3", "baz" "0"} {})
           {:common {}
            :added {}
            :removed {"foo/bar" "2.3", "baz" "0"}
            :changed ()})))
  (testing "change 1"
    (is (= (d/compare-dep-lists {"foo" "0"} {"foo" "1"})
           {:common {}
            :added {}
            :removed {}
            :changed [["foo" "0" "1"]]})))
  (testing "all 4"
    (is (= (d/compare-dep-lists
            {"com/mon" "2.3", "change" "0", "old" "2.a"}
            {"com/mon" "2.3", "change" "1", "new" "2.a"})
           {:common {"com/mon" "2.3"}
            :added {"new" "2.a"}
            :removed {"old" "2.a"}
            :changed [["change" "0" "1"]]})))
  (testing "real example"
    (is (= (d/compare-dep-lists
            {"org.clojure/clojure" "1.5.1"
             "org.timmc/handy" "1.7.0"
             "robert/hooke" "1.3.0"
             "org.clojure/algo.monads" "0.1.4"
             "org.clojure/tools.macro" "0.1.0"
             "org.clojars.runa/clj-schema" "0.9.3"}
            {"org.clojure/clojure" "1.6.0"
             "com.netflix.rxjava/rxjava-clojure" "0.19.1"
             "com.netflix.rxjava/rxjava-core" "0.19.1"
             "org.timmc/handy" "1.7.0"})
           {:changed [["org.clojure/clojure" "1.5.1" "1.6.0"]]
            :added {"com.netflix.rxjava/rxjava-clojure" "0.19.1"
                    "com.netflix.rxjava/rxjava-core" "0.19.1"}
            :removed {"robert/hooke" "1.3.0"
                      "org.clojure/algo.monads" "0.1.4"
                      "org.clojure/tools.macro" "0.1.0"
                      "org.clojars.runa/clj-schema" "0.9.3"}
            :common {"org.timmc/handy" "1.7.0"}}))))

(deftest locator-resolution
  (testing "git ref gets path"
    (is (= (d/resolve-locator "abcdef^")
           {:protocol :git
            :revspec "abcdef^:./project.clj"
            :spec "abcdef^"})))
  (testing "git index with path is unchanged"
    (is (= (d/resolve-locator ":foo/project.clj")
           {:protocol :git
            :revspec ":foo/project.clj"
            :spec ":foo/project.clj"})))
  (testing "file gets prefix stripped"
    (is (= (d/resolve-locator "file://foo/project.clj")
           {:protocol :file
            :file (java.io.File. "foo/project.clj")
            :spec "file://foo/project.clj"}))))
