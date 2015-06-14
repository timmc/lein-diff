# lein-diff

**This project is still in early and intermittent development. This
file is full of lies.**

A Leiningen plugin to perform diffs of dependencies between different
versions of a project.

## Usage

Put `[lein-diff "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
lein-diff 0.1.0-SNAPSHOT`.

Then execute this inside a project:

```bash
lein diff HEAD^ HEAD
```

...and... *something* comes out. At last revision of the README, you
get something like this:

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

## License

Copyright © 2015 Tim McCormack

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
