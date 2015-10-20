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
    (first (.intersectObjects raycaster objects))))




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
       (when-not (nil? target)
         (let [node (.-node (.-object target))]
           (go (>! chan {:type :select-node
                         :target node})))))
     false))


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
  (go (>! chan {:type :ready})))

