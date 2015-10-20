// BOOTSTRAP

//*
importScripts(  "../js/compiled/out/goog/base.js",
				"../js/compiled/out/goog/deps.js",
        "../js/compiled/out/goog/object/object.js",
				"../js/compiled/out/cljs_deps.js",
				"../js/compiled/out/cljs/core.js",
				"../js/compiled/out/gravity/force/worker.js"
);

goog.require("gravity.force.worker");
gravity.force.worker.main();
//*/

/*
importScripts("../out/gravity.js");

goog.require("gravity.force.worker");
gravity.force.worker.main();

//*/
