// BOOTSTRAP

//*
importScripts(  "../out/goog/base.js",
				"../out/goog/deps.js",
				"../out/cljs_deps.js",
				"../out/cljs/core.js",
				"../out/gravity/force/worker.js"
);

goog.require("gravity.force.worker");
gravity.force.worker.main();
//*/

/*
importScripts("../out/gravity.js");

goog.require("gravity.force.worker");
gravity.force.worker.main();

//*/