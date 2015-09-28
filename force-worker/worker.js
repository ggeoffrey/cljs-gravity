// BOOTSTRAP

importScripts(  "../out/goog/base.js",
				"../out/goog/deps.js",
				"../out/cljs_deps.js",
				"../out/cljs/core.js",
				"../out/force/worker.js"
);
//*
goog.require("force.worker");
force.worker.main();
//*/