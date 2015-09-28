(ns gravity.core
  (:require [clojure.browser.repl :as repl]
  			[gravity.view.graph :as graph]
     		[gravity.tools]))


;  (defonce conn
;     (repl/connect "http://localhost:9000/repl"))


(enable-console-print!)

(defn ^:export main
	"Entry point"
	[]
 	(graph/create))