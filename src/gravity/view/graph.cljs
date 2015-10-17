(ns gravity.view.graph
  (:require
   [gravity.tools :as t]
   [gravity.view.node :as node]
   [gravity.view.nodeset :as points]
   [gravity.force.proxy :as worker]
   [gravity.demo :as demo])
  (:require-macros [gravity.macros :refer [λ]]))




(defn fill-window!
  "Resize a canvas to make it fill the window"
  [canvas]
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height))
  nil)


(declare get-components)
(declare render-callback)
(declare onDocMouseMove)
(declare start-callback!)
(declare stop-callback!)
(declare resume-force-callback)
(declare add-background!)
(declare add-lights!)




(defn create
  "Initialise a context in the specified element id"
  [user-map dev-mode]
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



    (worker/listen force-worker (fn [event]
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

    (.addEventListener canvas "mousemove" (onDocMouseMove canvas camera raycaster colliders))

    (add-background! scene)
    (add-lights! scene)
    (render)

    {:start (start-callback! state render)
              :stop (stop-callback! state)
              :resume (resume-force-callback force-worker)
              :canvas (.-domElement renderer)
              :scene scene
              :stats (.-domElement stats)
              :camera camera
              :force-worker force-worker}

    ))


(defn get-components
  "Generate or re-use all the necessary components of the 3D view"
  [user-map dev-mode]
  (if dev-mode
    (fill-window! (:canvas user-map)))

  (let [width (.-width (:canvas user-map))
        height (.-height (:canvas user-map))
        camera (new js/THREE.PerspectiveCamera 75 (/ width height) 0.1 100000 )]

    ;; recover camera position

    (if (:camera-pos user-map)
      (.copy (.-position camera) (:camera-pos user-map))
      (set! (.-z (.-position camera))  50))


    ;; re-map
    {	:scene (new js/THREE.Scene)
      :width width
      :height height
      :camera camera
      :stats (t/make-stats)
      :controls (new js/THREE.OrbitControls camera)
      :renderer (new js/THREE.WebGLRenderer #js {"antialias" true
                                                 "canvas" (:canvas user-map)})
      :raycaster (new THREE.Raycaster)
      :classifier (.category10 js/d3.scale)
      :force-worker (if-not (nil? (:force-worker user-map))
                      (:force-worker user-map)
                      (worker/create "force-worker/worker.js"))
      :state (atom {:should-run true})}))


(defn- render-callback
  "Return a function rendering the context"
  [renderer scene camera stats state]
  (fn render []
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
  (fn []
    (swap! state assoc :should-run true)
    (render)
    nil))

(defn- stop-callback!
  "Return a closure affecting the application state atom"
  [state]
  (fn []
    (swap! state assoc :should-run false) nil))

(defn- resume-force-callback
  "Send a resume event to the force worker"
  [force-worker]
  (fn []
    (worker/send force-worker "resume")))



(defn onDocMouseMove  ; TODO : split in 2 and emit an event to an async channel
  "Callback for the mouseMove event on the canvas node"
  [canvas camera raycaster colliders]
  (let [mouse-pos (new js/THREE.Vector3)]
    (fn [event]
      (.preventDefault event)
      (let [bounding-rect (.getBoundingClientRect canvas)
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
        ;(log x y)
        (.set mouse-pos x y 1)
        (.unproject mouse-pos camera)
        ;(log mouse-pos)
        ;(log (.normalize (.sub mouse-pos cam-position)))
        (.set raycaster cam-position (.normalize (.sub mouse-pos cam-position)))
        (let [item (aget (.intersectObjects raycaster colliders) 0)]
          (when-not (nil? item)
            ;(t/log (-> item (.-object) (.-node)))
            )))
      false)))

(defn get-targets
  "Cast a ray to intersect objects under the mouse pointer.
  Return the first intersected or nil"
  []
  )





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
        (.add scene light))
      )
    nil))
