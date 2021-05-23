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

Build an uberjar and run `run.clj`.

## Results

See results directory

## Measurements

In [Zach Tellman's talk](https://www.youtube.com/watch?v=1bNOO3xxMc0) on
how exactly servers fall over, an important point is that it takes
*time*. In some experiments I ran, servers sometimes took minutes before
they started choking on the number of requests.

For this reason, I chose a minimal duration for wrk of 10 minutes. It is
not a lot. If anything, it's too short, but any less than that has been
found to provide incorrect results.

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
