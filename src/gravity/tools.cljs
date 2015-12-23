(ns gravity.tools)

(defn- get-args
  "Return the first arg or all the list as a js-obj"
  [coll]
  (if (= (count coll) 1)
    (clj->js (first coll))
   	(clj->js coll)))


(defn log
  "Log in the console"
  [& args]
  (.log js/console (get-args args))
	)

(defn warn
  "Warn in the console"
  [& args]
  (.warn js/console (get-args args)))
