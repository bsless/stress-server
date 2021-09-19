# stress-server

Stress testing and profiling different servers and the overhead of
routing frameworks.

For users, this project can help inform them with regards to best choice
of tool for the job and the best configuration for their needs.

For library and framework developers it can help point out performance
problems, provide a uniform analysis suite, and hopefully guide them in
optimization efforts.

Currently very work in progress, contributions and suggestions extremely welcome.

## Requirements

- This repo
- wrk
- hdr-plot
- babashka (for running the tests)

## Usage

- Build an uberjar
- Start a babashka nrepl server
- Connect to the server and load `./script/run.clj`
- Run `(-main {:url url :jar jar})`

## Results

See results directory

For more elaborate data analysis, see `./results/results.csv` which
contains the entire result set.

### Understanding the results

There are lots of files in the results directory.

- `out` files: Raw output from wrk2
- `png` files: hdr plots represented graphically
- `svg` files: flame graphs profiling of servers under stress test

Naming convention:

Let's look at an example file name: 

`httpkit.ring-middleware.async.java8.ParallelGC.r10k.t16.c400.d600s.png`

```
httpkit - server name
ring-middleware - handler
async - sync or async mode
java8 - java version
ParallelGC - GC algorithm
r10k - wrk rate
t16 - wrk threads number
c400 - wrk connections number
d600s - wrk duration in seconds
```

SVG files don't specify wrk parameters.

## Measurements

In [Zach Tellman's talk](https://www.youtube.com/watch?v=1bNOO3xxMc0) on
how exactly servers fall over, an important point is that it takes
*time*. In some experiments I ran, servers sometimes took minutes before
they started choking on the number of requests.

For this reason, I chose a minimal duration for wrk of 10 minutes. It is
not a lot. If anything, it's too short, but any less than that has been
found to provide incorrect results.

## Pitfalls, suggestions and conclusions

- async ring with http-kit. See `com.github.bsless.httpkit` ns
- Return raw bytes from muuntaja! `(m/create (assoc m/default-options :return :bytes))`
- Reitit options: `{:inject-match? false :inject-router? false}`
- Why did Aleph leak memory? Why did it stop? Could be related to reitit options?
- Ensure latest ring deps! Can be overridden by transitive dependencies.
- Regexes are bad on a hot path. So is `merge`.

### Surprising findings and quick wins

All of these turned into PRs

- found a bug in muuntaja where memoization did not work when content
  type was empty (merged)
- Keywordize keys has a huge cost (open)
- Ring params middleware was inefficient because it used `merge-with merge` (merged)
- Ring query params parser was slow because of it used regular
  expressions (merged)
- Ring content-type parsing was very slow (released long time ago,
  accidentally pulled in old dep)


## Winners

TBD
          
## TODO

- [X] Run with different Java versions
- [ ] GraalVM
- [ ] Java 16
- [ ] Loom
- [ ] Donkey
- [ ] Pedestal
- [X] Different GCs
- [ ] Lightweight routes
- [ ] Large responses
- [ ] Compojure
- [ ] Pohjavirta POST?
- [X] Rerun Aleph async
- [ ] Faster turnaround time
- [ ] Tuning?
- [ ] Push the envelope

## License

Copyright © 2021 Ben Sless

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
