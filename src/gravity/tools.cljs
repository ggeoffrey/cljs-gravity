(ns gravity.tools)

(defn- get-args
  "Return the first arg or all the list as a js-obj"
  [coll]
  (if (= (count coll) 1)
    (clj->js (first coll))
    (clj->js coll)))


(defn make-stats
  "Create a stat view to monitor performances"
  []
  (let [stats (new js/Stats)
        style (-> stats
                  (.-domElement)
                  (.-style))]
    (set! (.-position style) "absolute")
    (set! (.-left style) "0px")
    (set! (.-top style) "0px")
    stats))


(defn log
  "Log in the console"
  [& args]
  (.log js/console (get-args args)))

(defn warn
  "Warn in the console"
  [& args]
  (.warn js/console (get-args args)))
