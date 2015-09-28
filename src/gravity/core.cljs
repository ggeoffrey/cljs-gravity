(ns gravity.core
  (:require [gravity.view.graph :as graph]
     		[gravity.tools]))

;[clojure.browser.repl :as repl]
;  (defonce conn
;     (repl/connect "http://localhost:9000/repl"))


(enable-console-print!)

(defn ^:export main
	"Entry point"
	[]
 	(graph/create))