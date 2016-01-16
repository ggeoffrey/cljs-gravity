## A 3D force layout using D3.js and THREE.js

Cljs-gravity (please help me to find a better name!) is a ClojureScript library that plot an interactive graph, animated by a Barnes-Hut simulation. 

**This is a work in progress**, see the demo: http://ggeoffrey.github.io/ (Chrome or Chromium recomended ATM)

The goal is to make a safe and stable 3D graph visualisation that:
 - do one thing and do it well,
 - rely on quasi-standard tools,
 - ensure there is no side effects,
 - ensure there is no memory leaks,
 - use webworkers in an easy, safe and elegant way,
 - provide a rich set of events.

### Report
See [this document](https://github.com/ggeoffrey/cljs-gravity/raw/master/DRAFT-cljs-gravity.pdf)
for full knowledge on the library's content and rationals.
