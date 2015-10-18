(ns gravity.view.graph
  (:require
   [gravity.tools :refer [log]]
   [gravity.view.node :as node]
   [gravity.view.nodeset :as points]
   [gravity.view.tools :as tools]
   [gravity.view.events :as events]
   [gravity.force.proxy :as worker]
   [gravity.demo :as demo])
  (:require-macros [gravity.macros :refer [λ]]))






;; Init parameters for the view


(defn get-components
  "Generate or re-use all the necessary components of the 3D view"
  [user-map dev-mode]
  (if dev-mode
    (tools/fill-window! (:canvas user-map)))

  (let [width (.-width (:canvas user-map))
        height (.-height (:canvas user-map))
        camera (new js/THREE.PerspectiveCamera 75 (/ width height) 0.1 100000 )]

    {	:scene (new js/THREE.Scene)
      :width width
      :height height
      :camera camera
      :stats (:stats user-map)
      :controls (new js/THREE.OrbitControls camera)
      :renderer (new js/THREE.WebGLRenderer #js {"antialias" true
                                                 "canvas" (:canvas user-map)})
      :raycaster (new THREE.Raycaster)
      :classifier (:color user-map)
      :force-worker (:force-worker user-map)

      :state (atom {:should-run true})
      :first-run (:first-run user-map)}))







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
  (let [{	first-run :first-run
          scene :scene
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
        ;gen-nodes (clj->js (mapv #(js-obj) (range 0 1000)))
        {nodes :nodes
         meshes :meshes} (points/prepare-nodes (.-nodes data-test) classifier)

        links (.-links data-test)
        ;;nodeset (points/create nodes classifier)
        links-set (points/create-links nodes links)
        render (render-callback renderer scene camera stats state)]


    ;; renderer
    (.setSize renderer width height)
    (.setClearColor renderer 0x000000)

      ;;shadows
      (set! (.-enabled (.-shadowMap renderer)) true)
      (set! (.-type (.-shadowMap renderer)) js/THREE.PCFSoftShadowMap)

    (set! (.-z (.-position camera))  50)


    (worker/listen force-worker (λ [event]
                                   (let [message (.-data event)
                                         type (.-type message)
                                         data (.-data message)]
                                     (case type
                                       "nodes-positions" (do (points/update-positions! nodes data)
                                                           ;;(points/update nodeset)
                                                           (points/update links-set))))))



    ;; IF the app run for the first time in dev mode
    ;; OR in classic -not dev- mode
    (when (or (not dev-mode) first-run)
        (worker/send force-worker "set-nodes" (count nodes))
        (worker/send force-worker "set-links" links)
        (worker/send force-worker "start"))

    ;; if it's not the first time in dev mode
    (when (and (not first-run) dev-mode)
      (tools/fill-window! canvas)
      (.removeEventListener canvas "mousemove")
      (.removeEventListener canvas "click")
      (worker/send force-worker "tick"))




    ;(worker/send force-worker "precompute" 50)

    ;; adding nodes
    (doseq [item meshes]
      (.add scene item))

    ;; adding links
    (.add scene links-set)


    (.addEventListener canvas "mousemove" (events/onDocMouseMove canvas camera raycaster meshes chan))
    (.addEventListener canvas "click" (events/on-click canvas camera raycaster meshes chan))
    (.addEventListener js/window "resize" (events/onWindowResize canvas renderer camera))

    ;; add background
    (.add scene (tools/get-background))
    ;; add lights
    (doseq [light (tools/get-lights)]
      (.add scene light))

    (render)

    {:start (start-callback! state render)
     :stop (stop-callback! state)
     :resume (resume-force-callback force-worker)
     :canvas (.-domElement renderer)
     :stats stats}))










