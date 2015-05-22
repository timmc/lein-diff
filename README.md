# lein-diff

**This project is still in early and intermittent development. This
file is full of lies.**

A Leiningen plugin to perform diffs of dependencies between different
versions of a project.

## Development notes

From IRC:

> Writing lein-diff is more complicated than I thought. Dependencies
> get resolved in the context of a project; a project is loaded from
> file; the file *might* need to be in the context of the entire repo
> in case of lurking middleware.
> 
> It might have to be orchestrated by a shell script, I'm not sure.
> 
> Maybe you have a shell script that clones the project to two
> locations, checks out the appropriate commit in each, then calls
> lein diff with paths to the project.clj in each project. That lein
> diff then invokes lein diff from the working directory of each
> project to extract data, receives the results, and outputs the diff
> results.

## Usage

Put `[lein-diff "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
lein-diff 0.1.0-SNAPSHOT`.

Then execute this inside a project:

```bash
lein diff HEAD^ HEAD
```

## License

Copyright © 2015 Tim McCormack

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
