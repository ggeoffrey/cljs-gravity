(ns gravity.view.events
  "Events listeners on the canvas, mouse, etc…"
  (:require
   [gravity.tools :refer [log]]
   [gravity.view.tools :as tools]
   [cljs.core.async :refer [chan >! <! put!  sliding-buffer]])
  (:require-macros [gravity.macros :refer [λ]]
                   [cljs.core.async.macros :refer [go go-loop alt!]]))




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






(defn- move
  "Callback for the mouseMove event on the canvas node"
  [event canvas camera raycaster state chan events-state controls intersect-plane]
  (let [colliders (:meshes @state)
        target (get-target event canvas camera raycaster colliders)]
    (if-not (nil? target)
      (let [node (.-node (.-object target))]
        ;; disable controls
        (set! (-> controls .-enabled) false)
        ;; move plane
        (.copy (-> intersect-plane .-position) (-> node .-position))
        (.lookAt intersect-plane (-> camera .-position))
        ;; send event to the user
        (go
           (swap! events-state assoc :last :node-over)
           (>! chan {:type :node-over
                      :target node})))
      ;else
      (when (= :node-over (:last @events-state))
        (set! (-> controls .-enabled) true)
        (go (>! chan {:type :node-blur})
            (swap! events-state assoc :last :blur))))))


(defn- click
  "click event"
  [event canvas camera raycaster state chan]
  (let [colliders (:meshes @state)
        target (get-target event canvas camera raycaster colliders)]
    (when-not (nil? target)
      (let [node (-> target .-object .-node)]
        (go (>! chan {:type :node-click
                      :target node}))))
    false))


(defn double-click
  "Callback for the click event"
  [event canvas camera raycaster state chan]

  (let [colliders (:meshes @state)
        target (get-target event canvas camera raycaster colliders)]
    (when-not (nil? target)
      (let [node (.-node (.-object target))]
        (go (>! chan {:type :node-dbl-click
                      :target node})))))
  false)





(defn- drag
  [event canvas camera raycaster events-state intersect-plane]
  (let [node (:target @events-state)]
    (when-not (nil? node)
      (log :dragging)
      (let [node (-> node .-object)
            intersect (get-target event canvas camera raycaster (array intersect-plane))]
        (when-not (nil? intersect)
          (.copy (-> node .-position) (-> intersect .-point))
          )))))


(defn- down
  [event canvas camera raycaster state events-state]
  (let [colliders (:meshes @state)
        target (get-target event canvas camera raycaster colliders)]
    (when-not (nil? target)
      (swap! events-state assoc :target target))))


(defn- up
  [event events-state]
  (swap! events-state dissoc :target))




(defn notify-user-ready
  [chan]
  (go (>! chan {:type :ready})))






;; Events factory



(defn- listen-to-mouse-events
  "Take chans with events from the dom and alt! them to generate meaningful events."
  [mouse-down mouse-up mouse-move]
  (let [timeout-time 350

        out-chan (chan (sliding-buffer 1))
        events-state (atom {})]
    (go-loop []
             (loop []
               (alt! [mouse-down] ([transducted] (do
                                                   (swap! events-state assoc :event :down)
                                                   (go (>! out-chan (merge {:type :down} transducted)))

                                                   (js/setTimeout #(swap! events-state assoc :event nil) timeout-time)
                                                   nil))

                     ;; We stay in this loop while the mouse move without mousedown
                     [mouse-move] ([transducted] (do
                                                   (go (>! out-chan (merge {:type :move} transducted)))
                                                   (recur)))))
             (loop [nb-drags 0]
               (alt! [mouse-up] ([transducted] (do
                                                 (go (>! out-chan (merge {:type :up} transducted)))

                                                 (when (= :down (:event @events-state))
                                                   ;; the last event was a :down -> we trigger a click
                                                   ;; if we already had a click before it's a double-click
                                                   (if (and
                                                        (:last-was-a-click @events-state)
                                                        (= (:coords transducted) (:last-coords @events-state)))
                                                     (go
                                                      (swap! events-state assoc :last-was-a-click false)
                                                      (>! out-chan (merge {:type :double-click} transducted)))
                                                     ;else it's a simple click
                                                     (go (swap! events-state assoc :last-was-a-click true)
                                                         (>! out-chan (merge {:type :click} transducted))
                                                         (js/setTimeout #(swap! events-state assoc :last-was-a-click false) timeout-time))))

                                                 (swap! events-state assoc :event :up)
                                                 (swap! events-state assoc :last-coords (:coords transducted))

                                                 nil))
                     [mouse-move] ([transducted]
                                   (do
                                     (when (> nb-drags 3)
                                       (swap! events-state assoc :last-was-a-click false)
                                       (swap! events-state assoc :event :drag)
                                       (swap! events-state assoc :last-coords (:coords transducted))
                                       (go (>! out-chan (merge {:type :drag} transducted))))
                                     (recur (inc nb-drags))))))
             (recur))
    out-chan
    ))


(defn listen-to-canvas
  "Listen to a canvas and return a chan of events."
  [canvas]
  (let [transduct-mouse (map (λ [e]
                                ;;(.preventDefault e)
                                ;;(.stopPropagation e)
                                {:event e
                                 :coords {:x (.-offsetX e)
                                          :y (.-offsetY e)}}))
        mousedown-chan (chan (sliding-buffer 1) transduct-mouse)
        mouseup-chan (chan (sliding-buffer 1) transduct-mouse)
        mousemove-chan (chan (sliding-buffer 1) transduct-mouse)]

    (.addEventListener canvas "mousedown" (λ [e] (put! mousedown-chan e) false))
    (.addEventListener canvas "mousemove" (λ [e] (put! mousemove-chan e) false))
    (.addEventListener canvas "mouseup"   (λ [e] (put! mouseup-chan e) false))

    (listen-to-mouse-events mousedown-chan mouseup-chan mousemove-chan)))




(defn apply-events-to
  [mouse canvas camera raycaster intersect-plane controls state chan-out]
  (let [events-state (atom {})]
    (go-loop []
             (let [event (<! mouse)
                   {type :type
                    coords :coords
                    event :event} event]
               (case type
                 :move (do (move event canvas camera raycaster state chan-out events-state controls intersect-plane))
                 :down (do (down event canvas camera raycaster state events-state))
                 :up (do (up event events-state))
                 :click (do (click event canvas camera raycaster state chan-out))
                 :double-click (do (double-click event canvas camera raycaster state chan-out))
                 :drag (do (drag event canvas camera raycaster events-state intersect-plane ))
                 ))
             (recur)))
  nil)

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
      (set! (-> new-node .-selected) true)
      (.add (-> new-node .-mesh) circle))))


(defn watch-state
  [state watch-id]
  (add-watch state watch-id
             (λ [id state old-state new-state]
                (put-select-circle-on-node old-state new-state)
                )))
