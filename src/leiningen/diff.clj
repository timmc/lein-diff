(ns leiningen.diff
  (:require [leiningen.core.main :as lmain]
            leiningen.core.project
            [leiningen.core.classpath :as cp]
            [clojure.java.shell :as sh]
            [clojure.pprint :refer [pprint]]
            [clojure.set :as set]))

(defn git-locator-arg
  "Format a project locator for git."
  [locator]
  (str (:revision locator) ":" (:path locator)))

(defn fmt-locator
  "Format a project locator for display."
  [locator]
  (git-locator-arg locator))

;;;; Sourcing projects

(defn get-project-raw
  "From a git revision get the contents of a project file."
  [locator]
  ;; TODO err, exit
  (:out (sh/sh "git" "show" (git-locator-arg locator))))

;; Forked from leiningen.core.project/read, since we don't have an
;; actual file to work from.
(defn read-init-raw
  "Read project file without loading certificates, plugins, middleware, etc."
  [locator raw]
  (leiningen.core.project/init-project
   ;; read-raw introduced in 2.4.4
   (locking @(or (ns-resolve 'leiningen.core.project 'read-raw)
                 (ns-resolve 'leiningen.core.project 'read))
     (binding [*ns* (find-ns 'leiningen.core.project)]
       (try (eval (read-string raw)) ;; FIXME what about multiple forms?
            (catch Exception e
              (throw (Exception. (format "Error loading rev %s"
                                         (fmt-locator locator))
                                 e)))))
     (let [project (resolve 'leiningen.core.project/project)]
       (when-not project
         (throw (Exception. (format "%s must define project map"
                                    (fmt-locator locator)))))
       ;; return it to original state
       (ns-unmap 'leiningen.core.project 'project)
       @project))
   [:default]))

;;;; Dependencies extraction

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
       (map simple-dep)
       (into {})))

;;;; Processing

(defn compare-dep-lists
  [from to]
  (let [from-set (set (keys from))
        to-set (set (keys to))
        common (set/intersection from-set to-set)
        removed (set/difference from-set to-set)
        added (set/difference to-set from-set)
        changed (for [depname common
                      :let [from-ver (from depname)
                            to-ver (to depname)]
                      :when (not= from-ver to-ver)]
                  [depname from-ver to-ver])]
    {:common (select-keys from common)
     :removed (select-keys from removed)
     :added (select-keys to added)
     :changed changed}))

;;;; Entry point

(defn diff
  "Perform a diff of dependencies."
  [project from to]
  (let [deps-for (fn [rev]
                   (let [spec {:revision rev
                               :path "project.clj"}]
                     (get-deps (read-init-raw spec (get-project-raw spec)))))
        deps-from (deps-for from)
        deps-to (deps-for to)]
    (pprint (select-keys
             (compare-dep-lists deps-from deps-to)
             [:removed :added :changed]))))
