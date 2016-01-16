(ns gravity.force.proxy
	(:require [gravity.tools :refer [log warn err]]))


(defn send
  "Send a message to a given worker.
  arg worker : worker to target
  arg flag : a string (or keyword) that the worker can map to an action
  arg data : an object to send (optionnal)"
  ([worker flag]
   (.postMessage worker (clj->js {:type flag})))
  ([worker flag data]
   (.postMessage worker (clj->js { :type flag
                                   :data data}))))


(defn make-worker!
	[path]
	(log (str "A worker will be created to the given path '" path "'. If the canvas stay empty, be sure to check the path."))
	(new js/Worker path))

(defn create
  "Create a webworker with the given script path"
  [path params]
	(when-not path
		(warn (str "Invalid worker path ! Unable to create a graph without a correct worker file specified. (null)")))
  (let [worker (make-worker! path)]
		(if worker
			(send worker :init params)
			(err (str "Invalid worker for path '" path "'. Graph creation will fail.")))
    worker))

(defn listen
  "Listen to a given worker by setting the given function as a callback of onMessage"
  [worker callback]
  (.addEventListener worker "message" callback))


(defn serialize
  "Serialize a value (number, function, etc…) to be evaluated by another thread"
  [value]
  (str "(" (.toString value) ")"))
