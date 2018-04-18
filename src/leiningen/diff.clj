(ns leiningen.diff
  (:require [leiningen.core.main :as lmain]
            leiningen.core.project
            [leiningen.core.classpath :as cp]
            [clojure.java.shell :as sh]
            [clojure.pprint :refer [pprint]]
            [clojure.set :as set]))

;;;; Sourcing projects

(defn get-project-raw
  "From a locator get the contents of a project file."
  [locator]
  ;; TODO err, exit
  (case (:protocol locator)
    :file (slurp (:file locator) :encoding "UTF-8")
    :git (:out (sh/sh "git" "show" (:revspec locator)))))

;; Forked from leiningen.core.project/read, since we don't have an
;; actual file to work from.
(defn read-init-raw
  "Read project file without loading certificates, plugins,
middleware, etc. Locator given for messaging only."
  [raw locator]
  (leiningen.core.project/init-project
   ;; read-raw introduced in 2.4.4
   (locking @(or (ns-resolve 'leiningen.core.project 'read-raw)
                 (ns-resolve 'leiningen.core.project 'read))
     (binding [*ns* (find-ns 'leiningen.core.project)]
       (try (load-string raw) ;; MODIFIED: allows multiple forms
            (catch Exception e
              (throw (Exception. (format "Error loading rev %s"
                                         (:spec locator))
                                 e)))))
     (let [project (resolve 'leiningen.core.project/project)]
       (when-not project
         (throw (Exception. (format "%s must define project map"
                                    (:spec locator)))))
       ;; return it to original state
       (ns-unmap 'leiningen.core.project 'project)
       @project))))

;;;; Dependencies extraction

(defn graph-nodes
  "Given a dependency graph, yield all the dependencies."
  [graph]
  (for [[k sub] graph
        node (cons k (graph-nodes sub))]
    node))

(defn simple-dep
  "Simplify a dependency vector into just the first two elements, name
  and version."
  [dep]
  [(first dep) (second dep)])

(defn get-deps
  "Given a project, yield all (transitive) dependencies as a map of
names to versions."
  [project]
  (->> (cp/dependency-hierarchy :dependencies project)
       graph-nodes
       (map simple-dep)
       (into {})))

;;;; Processing

(defn compare-dep-lists
  "Given two dependency maps, yield a map of:

- :common - Submap of common dependencies
- :removed - Submap of deps only in `from`
- :added - Submap of deps only in `to`
- :changed - Coll of vectors [depname, from-version, to-version] where
  the versions differ."
  [from to]
  (let [from-set (set (keys from))
        to-set (set (keys to))
        removed-names (set/difference from-set to-set)
        added-names (set/difference to-set from-set)
        common-names (set/intersection from-set to-set)
        changed (for [depname common-names
                      :let [from-ver (from depname)
                            to-ver (to depname)]
                      :when (not= from-ver to-ver)]
                  [depname from-ver to-ver])
        common (into {} (set/intersection (set from) (set to)))]
    {:common common
     :removed (select-keys from removed-names)
     :added (select-keys to added-names)
     :changed changed}))

;;;; Entry point

(defn resolve-locator
  "Given a loose project locator, yield a well-defined one. (Specificy
path to project.clj if not given.)"
  [locator-spec]
  (let [file-proto-prefix "file://"]
    (assoc (if (.startsWith locator-spec file-proto-prefix)
             {:protocol :file
              :file (java.io.File. (.substring locator-spec
                                               (count file-proto-prefix)))}
             {:protocol :git
              :revspec (if (.endsWith locator-spec "project.clj")
                         locator-spec
                         (str locator-spec ":./project.clj"))})
      :spec locator-spec)))

(defn deps-for-rev
  "Given a revision, yield dependencies in project (including
transitive) as a map of depnames to versions."
  [locator-spec]
  (let [locator (resolve-locator locator-spec)]
    (get-deps (read-init-raw (get-project-raw locator) locator))))

(defn ^:no-project-needed diff
  "Perform a diff of leiningen projects."
  [project from to]
  (pprint (select-keys (compare-dep-lists (deps-for-rev from)
                                          (deps-for-rev to))
                       [:removed :added :changed])))
