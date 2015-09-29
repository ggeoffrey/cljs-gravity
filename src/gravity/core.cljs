(ns gravity.core
	(:require [gravity.view.graph :as graph]))

;[clojure.browser.repl :as repl]
;(repl/connect "http://localhost:9000/repl")


(enable-console-print!)

(defn ^:export main
	"Entry point"
	[]
	(graph/create))