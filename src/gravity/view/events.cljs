(ns gravity.view.events
  "Events listeners on the canvas, mouse, etc…"
  (:require
   [gravity.tools :refer [log]]
   [gravity.view.tools :as tools]
   [cljs.core.async :refer [>!]])
  (:require-macros [gravity.macros :refer [λ]]
                   [cljs.core.async.macros :refer [go]]))





(defn- get-target
  "Cast a ray to intersect objects under the mouse pointer.
  Return the first intersected or nil"
  [event canvas camera raycaster objects]
  (when-not (or (nil? objects) (empty? objects))
    (let [mouse-pos (new js/THREE.Vector3)
          bounding-rect (.getBoundingClientRect canvas)
          x (-> (.-clientX event)
                (- (.-left bounding-rect))
                (/ (.-offsetWidth canvas))
                (* 2)
                (- 1))
          y (-> (.-clientY event)
                (- (.-top bounding-rect))
                (/ (.-offsetHeight canvas))
                (-)
                (* 2)
                (+ 1))
          cam-position (.-position camera)]
      (.set mouse-pos x y 1)
      (.unproject mouse-pos camera)
      (.set raycaster cam-position (.normalize (.sub mouse-pos cam-position)))
      ;;return
      (first (.intersectObjects raycaster objects)))))






(defn onDocMouseMove
  "Callback for the mouseMove event on the canvas node"
  [canvas camera raycaster state chan]
  (λ [event]
     (.preventDefault event)
     (let [colliders (:meshes @state)
           target (get-target event canvas camera raycaster colliders)]
       (if-not (nil? target)
         (let [node (.-node (.-object target))]
           (go (>! chan {:type :mouse-in-node
                         :target node})))
         ;else
         (go (>! chan {:type :mouse-out-node}))))
     false))


(defn on-click
  "Callback for the click event"
  [canvas camera raycaster state chan]
  (λ [event]
     (.preventDefault event)
     (let [colliders (:meshes @state)
           target (get-target event canvas camera raycaster colliders)]
       (if-not (nil? target)
         (let [node (.-node (.-object target))]
           (go (>! chan {:type :node-click
                         :target node})))
         ;else
         (do
           ;(swap! state assoc :selected nil)
           ;(go (>! chan {:type :voidclick})))
           ))
       false)))


(defn onWindowResize
  "Callback for the window-resize event"
  [canvas renderer camera]
  (λ []
     (tools/fill-window! canvas)
     (let [width (.-innerWidth js/window)
           height (.-innerHeight js/window)]
       (set! (.-aspect camera) (/ width height))
       (.updateProjectionMatrix camera)
       (.setSize renderer width height))
     false))


(defn notify-user-ready
  [chan]
  (go (>! chan {:type "ready"})))






  ;; State watch


(defn put-select-circle-on-node
  [old-state new-state]
  (let [circle (:select-circle new-state)
        old-node (:selected old-state)
        new-node (:selected new-state)]
    (when-not (nil? old-node)
      (set! (-> old-node .-selected) false)
      (.remove (-> old-node .-mesh) circle))
    (when-not (nil? new-node)
      (.add (-> new-node .-mesh) circle))))


(defn watch-state
  [state watch-id]
  (add-watch state watch-id
             (λ [id state old-state new-state]
                (put-select-circle-on-node old-state new-state)
                )))
