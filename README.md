# lein-diff

A Leiningen plugin to perform diffs of transitive dependencies between
different versions of a project.

**This project is still in early and intermittent development. Expect
the interface to change drastically between versions until 1.0 is
release.**

Ask for a diff between two git revisions:

```bash
lein diff HEAD^ HEAD
```

and out comes a map of differences:

```clojure
{:changed ([org.clojure/clojure "1.5.1" "1.6.0"]),
 :added
 {com.netflix.rxjava/rxjava-clojure "0.19.1",
  com.netflix.rxjava/rxjava-core "0.19.1"},
 :removed
 {robert/hooke "1.3.0",
  org.clojure/algo.monads "0.1.4",
  org.clojure/tools.macro "0.1.0",
  org.clojars.runa/clj-schema "0.9.3"}}
```

[![Clojars Project](https://clojars.org/org.timmc/lein-diff/latest-version.svg)](https://clojars.org/org.timmc/lein-diff)

## Motivation

Looking at diffs of a project.clj only tells you what explicit
dependencies have changed, not how your transitive dependencies have
changed. What if the new version of clj-http brought in a new version
of ApacheHttpClient with different behavior? What if reordering your
dependencies changed which version of a transitive dep was pulled in?

lein-diff is intended to ferret out these differences. The goal is to
support build infrastructure such that (for example) a build agent can
annotate a GitHub pull request with a list of changed dependencies, or
support scripting such that a git bisect command can determine where a
transitive dependency changed.

## Usage

Put `[lein-diff "0.1.0"]` into the `:plugins` vector of your
`:user` profile.

CLI syntax: `lein diff <from> <to>`

`<from>` and `<to>` are locators for project.clj files:

- `<revspec>` - A git revision specifier (see `man 7 gitrevisions`)
  naming a commit. lein-diff will load the project.clj in the current
  directory from that revision. (Equivalent to specifying a path of
  `project.clj` in the next form.)
- `<revspec>:<repo-path>` - A revision specifier naming a blob using
  revision + path such as `HEAD:project.clj`,
  `ba68a0:common/project.clj`, or `my-branch~3:project.clj`.
- `file://<path>` - A file path of `file://project.clj` takes
  project.clj from the current directory. Note the lack of a third
  slash, which would indicate an absolute path. Alpha feature; does
  not yet actually work like a URI, so don't use percent-encoding!

The output is a pretty-printed map. The `:added` and `:removed` values
are maps of dependency names to the versions newly added or last seen;
the `:changed` value is a sequence of vectors containing the
dependency name, the previous version, and the new version.

### Examples

- `lein diff abcdef^ abcdef` shows what commit `abcdef` changed
  relative to its first parent
- `lein diff $(git merge-base master HEAD) HEAD` shows the changes
  you've made since forking from master (but not staged changes or
  changes in the working directory)
- `lein diff HEAD ""` shows the staged changes to project.clj (`:path`
  specifies a blob in the index in git)
- `lein diff move-it^:old-path/project.clj move-it:new-path/project.clj`
  compares a project.clj file that moved during the last commit on
  a branch.
- `lein diff HEAD file://project.clj` shows the uncommitted changes to
  project.clj

## TODO

- Allow specifying weird branches and paths that would make for
  ambiguous or unusable git revspecs. (Or at least document exactly
  what is not supported.)
- Allow references to other SCMs... or maybe just command outputs.

## License

Copyright © 2015 Tim McCormack

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
