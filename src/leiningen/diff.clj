(ns leiningen.diff
  (:require [leiningen.core.classpath :as cp]
            [clojure.pprint :refer [pprint]]))

(defn graph-nodes
  [graph]
  (for [[k sub] graph
        node (cons k (graph-nodes sub))]
    node))

(defn simple-dep
  [dep]
  [(first dep) (second dep)])

(defn get-deps
  [project]
  (->> (cp/dependency-hierarchy :dependencies project)
       graph-nodes
       (map simple-dep)))

(defn diff
  "Perform a diff of dependencies."
  [project from to]
  (pprint (get-deps project)))
