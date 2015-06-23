(ns leiningen.t-diff
  (:require [leiningen.diff :as d]
            [clojure.test :refer :all]))

(deftest locator-resolution
  (is (= (d/resolve-locator "abcdef")
         "abcdef:./project.clj"))
  (is (= (d/resolve-locator "abcdef:foo/project.clj")
         "abcdef:foo/project.clj")))
