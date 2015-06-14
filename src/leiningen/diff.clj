(ns leiningen.diff
  (:require [leiningen.core.main :as lmain]
            leiningen.core.project
            [leiningen.core.classpath :as cp]
            [clojure.java.shell :as sh]
            [clojure.pprint :refer [pprint]]))

(defn fmt-locator
  [locator]
  (str (:revision locator) ":" (:path locator)))

(defn git-locator-arg
  [locator]
  (fmt-locator locator))

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

;;;; Processing

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
  (let [deps-for (fn [rev]
                   (let [spec {:revision rev
                               :path "project.clj"}]
                     (get-deps (read-init-raw spec (get-project-raw spec)))))
        deps-from (deps-for from)
        deps-to (deps-for to)]
    (println "FROM:")
    (pprint deps-from)
    (println "TO:")
    (pprint deps-to)))
