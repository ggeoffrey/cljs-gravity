(.importScripts js/self "../libs/d3.min.js")

;;---------------------------------

(ns gravity.force.worker)

(defn answer
  "Post a message back"
  ([message]
   	(.postMessage js/self (clj->js message)))
  ([message data]
  	(.postMessage js/self (clj->js message) (clj->js data))))

(defn log 
  "Log in the console"
  [args]
  (.log js/console "[force.worker/log]: " args))

(defn warn 
  "Warn in the console"
  [args]
  (.warn js/console "[force.worker/warn]: " args))

(defn str
	[& args]
	(let [arr (clj->js args)]
		(.join arr "")))



(def force nil)

;; --------------------------------



(declare tick)


(defn dispatcher 
  "Dispatch a message to the corresponding action (route)."
  [event]
  
  (let [message (.-data event)
        type (.-type message)
        data (.-data message)]
    (case type
      "start" (start)
      "stop"  (stop)
      "set-nodes" (set-nodes data)
      "set-links" (set-links data)
      "precompute" (precompute data)
      (warn (str "Unable to dispatch '" type "'")))))


(defn ^:export main
  "Main entry point"
  []  
  (def force (.force (.-layout js/d3)))
  (.on force "tick" tick)
  (.addEventListener js/self "message" dispatcher))



(defn start 
  "start the force"
  []
  (log "starting force")
  (.start force))

(defn stop 
  "Stop the force"
  []
  (.stop force))

(defn set-nodes 
  "Set the nodes list"
  [nodes]
  (.nodes force nodes))

(defn set-links
  "Set the links list"
  [links]
  (.links force links))


(defn precompute 
  	"Force the layout to precompute"
	[steps]
	(if (or (< steps 0) (nil? steps))
   		(do
       		;;(log "PAS OK")
       		(.log js/console "Precomputing layout with default value. Argument given was <0. Expected unsigned integer, Given:" steps )
         	(precompute 50))
     	(do
        	(let [start (.now js/Date)]
        		(.on force "tick" nil)
	      		(dotimes [i steps]
					(.tick force))
	       		(.on force "tick" tick)
        		(log (str "Pre-computed in " (/ (- (.now js/Date) start) 1000) "ms.")))
        	)))


(defn tick 
  "Tick function for the force layout"
  [_]
  (let [nodes (.nodes force)
        size (dec (.-length nodes))
        arr (new js/Float32Array (* size 3))
        buffer (.-buffer arr)]
    (loop [i 0]
        (let [j (* i 3)
           	  node (aget nodes i)]
	      (aset arr j (.-x node))
	      (aset arr (+ j 1) (.-y node))
	      (if (js/isNaN (.-z node))
	      	(aset arr (+ j 2) 0)
	      	(aset arr (+ j 2) (.-z node))))
		(when (< i size)
        	(recur (inc i))))

    (answer {:type "nodes-positions"
             :data arr}
            [buffer])))



;; START

(main)