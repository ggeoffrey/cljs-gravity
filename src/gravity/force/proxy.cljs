(ns gravity.force.proxy
	(:require [gravity.tools]))

(defn create
  "Create a webworker with the given script path"
  [path]
  (new js/Worker path))

(defn listen
  "Listen to a given worker by setting the given function as a callback of onMessage"
  [worker callback]
  (.addEventListener worker "message" callback))

(defn send
  "Send a message to a given worker.
  @arg worker : worker to target
  @arg flag : a string that the worker can map to an action
  @arg data : an object to send (optionnal)"
  ([worker flag]
   (.postMessage worker (clj->js {:type flag})))
  ([worker flag data]
   (.postMessage worker (clj->js { :type flag
                                   :data data}))))
