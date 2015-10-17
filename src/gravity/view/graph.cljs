(ns gravity.view.graph
  (:require
   [gravity.tools :as t]
   [gravity.view.node :as node]
   [gravity.view.nodeset :as points]
   [gravity.force.proxy :as worker]
   [gravity.demo :as demo]
   [cljs.core.async :refer [>!]])
  (:require-macros [gravity.macros :refer [λ]]
                   [cljs.core.async.macros :refer [go]]))





;; Tools for the DOM

(defn fill-window!
  "Resize a canvas to make it fill the window"
  [canvas]
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height))
  nil)



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


;; Init parameters


(defn get-components
  "Generate or re-use all the necessary components of the 3D view"
  [user-map dev-mode]
  (if dev-mode
    (fill-window! (:canvas user-map)))

  (let [width (.-width (:canvas user-map))
        height (.-height (:canvas user-map))
        camera (new js/THREE.PerspectiveCamera 75 (/ width height) 0.1 100000 )]

    {	:scene (new js/THREE.Scene)
      :width width
      :height height
      :camera camera
      :stats (if-not (nil? (:stats user-map))
               (:stats user-map)
               (make-stats))
      :controls (new js/THREE.OrbitControls camera)
      :renderer (new js/THREE.WebGLRenderer #js {"antialias" true
                                                 "canvas" (:canvas user-map)})
      :raycaster (new THREE.Raycaster)
      :classifier (.category10 js/d3.scale)
      :force-worker (if-not (nil? (:force-worker user-map))
                      (:force-worker user-map)
                      (worker/create "force-worker/worker.js"))
      :state (atom {:should-run true})}))



;; TOOLS the scene


(defn- add-background!
  "Generate a gray sphere as a background"
  [scene]
  (let [material (new js/THREE.MeshLambertMaterial #js {"color" 0xa0a0a0
                                                        ;"ambiant"  0xffffff
                                                        "side" 1})
        geometry (new js/THREE.SphereGeometry 20 20 20)
        background (new js/THREE.Mesh geometry material)]
    (.set (.-scale background) 200 200 200)
    (.add scene background)))


(defn add-lights!
  "Add lights into the scene"
  [scene]
  (let [color (new js/THREE.Color 0xffffff)
        strength   0.6
        shadow-map 2048
        positions [[3000 3000 3000]
                   [-3000 3000 3000]
                   [-3000 -3000 3000]
                   [3000 -3000 3000]

                   [3000 3000 -3000]]]
    (doseq [pos positions]
      (let [light (new js/THREE.SpotLight color strength)]
        (.set (.-position light) (first pos) (nth pos 1) (last pos))
        (.add scene light)))
    nil))




;; EVENTS



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




(defn- onDocMouseMove
  "Callback for the mouseMove event on the canvas node"
  [canvas camera raycaster colliders chan]
  (λ [event]
     (.preventDefault event)
     (let [target (get-target event canvas camera raycaster colliders)]
       (if-not (nil? target)
         (let [node (.-node (.-object target))]
           (go (>! chan {:type :mouse-in-node
                         :target node})))
         ;else
         (go (>! chan {:type :mouse-out-node}))))
     false))









;; CALLBACKS







(defn- render-callback
  "Return a function rendering the context"
  [renderer scene camera stats state]
  (λ render []
     (when-not (nil? stats)
       (.begin stats))

     (if (get @state :should-run)
       (do
         (.requestAnimationFrame js/window render)
         (.render renderer scene camera)))

     (when-not (nil? stats)
       (.end stats))))




(defn- start-callback!
  "Return a closure affecting the application state atom
  and triggering the render function once"
  [state render]
  (λ []
     (swap! state assoc :should-run true)
     (render)
     nil))




(defn- stop-callback!
  "Return a closure affecting the application state atom"
  [state]
  (λ []
     (swap! state assoc :should-run false) nil))

(defn- resume-force-callback
  "Send a resume event to the force worker"
  [force-worker]
  (λ []
     (worker/send force-worker "resume")))











;; USE ALL THE ABOVE




(defn create
  "Initialise a context in the specified element id"
  [user-map chan dev-mode]
  (let [{	scene :scene
          width :width
          height :height
          camera :camera
          stats :stats
          controls :controls
          renderer :renderer
          raycaster :raycaster
          classifier :classifier
          force-worker :force-worker
          state :state} (get-components user-map dev-mode)
        canvas (if-not (nil? (:canvas user-map))
                 (:canvas user-map)
                 (.-domElement renderer))
        data-test (demo/get-demo-graph)
        ;gen-nodes (clj->js (mapv #(js-obj) (range 0 2000)))
        {nodes :nodes
         colliders :colliders} (points/prepare-nodes (.-nodes data-test) classifier)

        links (.-links data-test)
        nodeset (points/create nodes classifier)
        links-set (points/create-links nodes links)
        render (render-callback renderer scene camera stats state)]


    (.setSize renderer width height)
    (.setClearColor renderer 0x000000)
    (set! (.-z (.-position camera))  50)


    (worker/listen force-worker (λ [event]
                                   (let [message (.-data event)
                                         type (.-type message)
                                         data (.-data message)]
                                     (case type
                                       "nodes-positions" (do (points/update-positions! nodes data)
                                                           (points/update nodeset)
                                                           (points/update links-set))))))


    ;(worker/send force-worker "select-mode" "2D")

    (if-not dev-mode
      (do
        (worker/send force-worker "set-nodes" (.-length nodes))
        (worker/send force-worker "set-links" links)
        (worker/send force-worker "start"))
      (do
        (fill-window! canvas)
        (worker/send force-worker "tick")))


    ;(worker/send force-worker "precompute" 50)


    ;(.add scene nodeset)

    (loop [i 0]
      (.add scene (aget colliders i))
      (when (< i (dec (.-length colliders)))
        (recur (inc i))))

    ;(.add scene colliders)
    (.add scene links-set)

    (.addEventListener canvas "mousemove" (onDocMouseMove canvas camera raycaster colliders chan))

    (add-background! scene)
    (add-lights! scene)
    (render)

    {:start (start-callback! state render)
     :stop (stop-callback! state)
     :resume (resume-force-callback force-worker)
     :canvas (.-domElement renderer)
     :scene scene
     :stats (.-domElement stats)}

    ))










