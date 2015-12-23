(ns gravity.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! >! chan]]
            [gravity.tools :refer [log]]))



(defn create-chan
  "Return a simple chan
  - avoiding a :require into core
  - centralizing the creation"
  []
  (chan 512))


(defn create-store
  "Create an atom and two closures ':on' and ':get-callbacks'.
  - Use :on to store a callback
  - Use :get-callbacks to retrive a deref of the callbacks atom map.
  Warning : the keys are transformed to keywords."
  []
  (let [callbacks-map (atom {})]

    ;;return 'on'
    {:on (fn [cb-key callback]
            (swap! callbacks-map assoc (keyword cb-key) callback))
     :off (fn
            ([]
             (reset! callbacks-map {}))
            ([cb-key]
             (swap! callbacks-map assoc (keyword cb-key) nil)) )

     :get-callbacks (fn [] @callbacks-map)}))




;; Events triggers

(defn- get-callbacks
  "Extract and execute the :get-callbacks closure from the store"
  [store]
  (let [deref-callbacks (:get-callbacks store)]
    (deref-callbacks)))



(defn- call
  [callback & args]
  (when-not (nil? callback)
    (apply callback args)))


(defn- trigger
  "Trigger an event taking no arguments"
  ([callback-key store]
   (trigger callback-key store nil nil))

	([callback-key store object-to-pass]
	 (trigger callback-key store object-to-pass nil))

  ([callback-key store object-to-pass original-event]
   (let [store (get-callbacks store)
         callback (callback-key store)]
     (call callback object-to-pass original-event))))






;; dispatcher

(defn listen-outgoing-events
  "Listen to an output chan and trigger the appropriate callbacks if found in the events-store."
  [chan-out store]
  (let [state (atom {:target nil})]
    (go
     (while true
       (let [event (<! chan-out)
             node (:target event)
						 original-event (:original-event event)]
         (case (:type event)
           :ready (trigger :ready store)
           :node-over (do
                            (swap! state assoc :target node)
                            (trigger :node-over store node original-event))
           :node-blur (do
                             (swap! state assoc :target nil)
                             (trigger :node-blur store original-event))
           :node-select (do
                          (swap! state assoc :selected node)
                          (trigger :node-select store node original-event))

           :node-click (trigger :node-click store node original-event)
           :node-dbl-click (trigger :node-dbl-click store node original-event)
           :void-click (trigger :void-click store original-event)
           :drag-start (trigger :drag-start store node original-event)
           :drag-end   (trigger :drag-end store node original-event)
           nil)
         )))))





