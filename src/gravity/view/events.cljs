(ns gravity.view.events
  "Events listeners on the canvas, mouse, etc…"
  (:require
   [gravity.tools :refer [log]]
   [gravity.view.tools :as tools]
   [cljs.core.async :refer [chan >! <! put!  sliding-buffer]])
  (:require-macros [gravity.macros :refer [λ]]
                   [cljs.core.async.macros :refer [go go-loop alt!]]))





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
           (go (>! chan {:type :node-over
                         :target node})))
         ;else
         (go (>! chan {:type :node-blur}))))
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


(defn on-dbl-click
  "Callback for the click event"
  [canvas camera raycaster state chan]
  (λ [event]
     (.preventDefault event)
     (let [colliders (:meshes @state)
           target (get-target event canvas camera raycaster colliders)]
       (if-not (nil? target)
         (let [node (.-node (.-object target))]
           (go (>! chan {:type :node-dbl-click
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
  (go (>! chan {:type :ready})))






;; Events factory



(defn- listen-to-mouse-events
  "Take chans with events from the dom and alt! them to generate meaningful events."
  [mouse-down mouse-up mouse-move]
  (let [timeout-time 350

        out-chan (chan 1)
        events-state (atom {})]
    (go-loop []
             (loop []
               (alt! [mouse-down] ([coords] (do
                                              (swap! events-state assoc :event :down)
                                              (go (>! out-chan {:type :down
                                                                :coords coords}))
                                              (js/setTimeout #(swap! events-state assoc :event nil) timeout-time)
                                              nil))))
             (loop [nb-drags 0]
               (alt! [mouse-up] ([coords] (do



                                            (go (>! out-chan {:type :up
                                                              :coords coords}))

                                            (when (= :down (:event @events-state))
                                              ;; the last event was a :down -> we trigger a click
                                              ;; if we already had a click before it's a double-click
                                              (if (and
                                                   (:last-was-a-click @events-state)
                                                   (= coords (:last-coords @events-state)))
                                                (go (log coords (:last-coords @events-state))
                                                    (swap! events-state assoc :last-was-a-click false)
                                                    (>! out-chan {:type :double-click
                                                                  :coords coords}))
                                                ;else it's a simple click
                                                (go (swap! events-state assoc :last-was-a-click true)
                                                    (>! out-chan {:type :click
                                                                  :coords coords})
                                                    (js/setTimeout #(swap! events-state assoc :last-was-a-click false) timeout-time))))

                                            (swap! events-state assoc :event :up)
                                            (swap! events-state assoc :last-coords coords)

                                            nil))
                     [mouse-move] ([coords]
                                   (do
                                     (when (> nb-drags 3)
                                       (swap! events-state assoc :last-was-a-click false)
                                       (swap! events-state assoc :event :drag)
                                       (swap! events-state assoc :last-coords coords)
                                       (go (>! out-chan {:type :drag
                                                         :coords coords})))
                                     (recur (inc nb-drags))))))
             (recur))
    out-chan
    ))


(defn listen-to-canvas
  "Listen to a canvas and return a chan of events."
  [canvas]
  (let [transduct-mouse (map (λ [e] {:x (.-offsetX e) :y (.-offsetY e)}))
        mousedown-chan (chan 1 transduct-mouse)
        mouseup-chan (chan 1 transduct-mouse)
        mousemove-chan (chan (sliding-buffer 1) transduct-mouse)]

    (.addEventListener canvas "mousedown" #(put! mousedown-chan %))
    (.addEventListener canvas "mousemove" #(put! mousemove-chan %))
    (.addEventListener canvas "mouseup" #(put! mouseup-chan %))

    (let [mouse (listen-to-mouse-events mousedown-chan mouseup-chan mousemove-chan)]
      (go-loop []
               (let [event (<! mouse)]
                 (log event))
               (recur))

      nil)))






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
