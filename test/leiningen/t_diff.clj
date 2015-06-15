(ns leiningen.t-diff
  (:require [leiningen.diff :as d]
            [clojure.test :refer :all]))

(deftest locator-formatting
  (is (= (d/git-locator-arg {:revision "HEAD" :path "project.clj"})
         "HEAD:project.clj")))
