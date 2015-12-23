(ns gravity.view.graph
  (:require
   [gravity.tools :refer [log]]
   [gravity.view.node :as node]
   [gravity.view.nodeset :as points]
   [gravity.view.tools :as tools]
   [gravity.view.events :as events]
   [gravity.force.proxy :as worker]))






;; Init parameters for the view


(defn get-components
  "Generate or re-use all the necessary components of the 3D view"
  [user-map dev-mode]
  (if dev-mode
    (tools/fill-window! (:canvas user-map)))

  (let [webgl-params (:webgl user-map)
        width (.-width (:canvas user-map))
        height (.-height (:canvas user-map))
        camera (new js/THREE.PerspectiveCamera 75 (/ width height) 0.1 100000 )]

    (set! (.-z (.-position camera))  300)


    {	:scene (new js/THREE.Scene)
      :width width
      :height height
      :camera camera
      :stats (:stats user-map)
      :controls (new js/THREE.OrbitControls camera)
      :renderer (new js/THREE.WebGLRenderer #js {:antialias (:antialias webgl-params)
                                                 :canvas (:canvas user-map)})
      :raycaster (new THREE.Raycaster)
      :classifier (:color user-map)
      :force-worker (if (:force-worker user-map)
                      (:force-worker user-map)
                      (worker/create "force-worker/worker.js" (:force user-map)))

      :state (atom {:should-run true})
      :first-run (:first-run user-map)}))







;; CALLBACKS



(defn- render-callback
  "Return a function rendering the context"
  [renderer scene camera stats state controls select-circle]
  (fn render []

     (.update controls)

     (when-not (nil? stats)
       (.begin stats))

     (if (get @state :should-run)
       (do
         (when-not (nil? select-circle)
           (let [x1 (-> select-circle .-rotation .-x)
                 y1 (-> select-circle .-rotation .-y)
                 x2 (+ x1 0.01)
                 y2 (+ y1 0.1)]
             (.set (-> select-circle .-rotation) x2 y2 0)))
         (.requestAnimationFrame js/window render)
         (.render renderer scene camera)))

     (when-not (nil? stats)
       (.end stats))))




(defn- start-callback!
  "Return a closure affecting the application state atom
  and triggering the render function once"
  [state render]
  (fn []
     (when-not (:should-run @state)
       (swap! state assoc :should-run true)
       (render))
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





(defn- set-links
  "Remove the old links and add the new ones"
  [state scene links]
  (let [old-links (:links-set @state)
        nodes (:nodes @state)
        new-links (points/create-links nodes links)]
    (.remove scene old-links)
    (.add scene new-links)
    (swap! state assoc :links links)
    (swap! state assoc :links-set new-links)
    links))

(defn- set-nodes
  "Remove the old nodes and add the new ones"
  [state scene nodes]
  (let [classifier (:classifier @state)
        old-nodes (:nodes @state)
        {nodes :nodes
         meshes :meshes} (points/prepare-nodes nodes classifier)]
    (doseq [old old-nodes]
      (.remove scene (-> old .-mesh)))
    (doseq [new-node meshes]
      (.add scene new-node))
    (swap! state assoc :nodes nodes)
    (swap! state assoc :meshes meshes)
    (set-links state scene [])
    nodes))



(defn- set-nodes-callback
  "Allow the user to replace nodes with a new set of nodes"
  [state scene]
  (fn [nodes]
     (if (nil? nodes)
       (:nodes @state)
       (set-nodes state scene nodes))))

(defn- set-links-callback
  [state scene]
  (fn [links]
     (if (nil? links)
       (:links @state)
       (set-links state scene links))))




(defn- update-force-callback
  [state force-worker]
  (fn []
     (let [nodes (:nodes @state)
           links (:links @state)]
       (worker/send force-worker "set-nodes" (count nodes))
       (worker/send force-worker "set-links" links)
       ;;(worker/send force-worker "start")
       )))









;; USE ALL THE ABOVE


(defn init-force
  [force-worker dev-mode first-run]
  (when (and (not first-run) dev-mode)
    (worker/send force-worker "tick")))


(defn create
  "Initialise a context in the specified element id"
  [user-map chan-out dev-mode]
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

        select-circle (tools/get-circle)
        intersect-plane (tools/get-intersect-plane)
        ;;data-test (demo/get-demo-graph)
        ;;{nodes :nodes
        ;; meshes :meshes} (points/prepare-nodes (.-nodes data-test) classifier)

        ;;links (.-links data-test)
        ;;nodeset (points/create nodes classifier)
        ;;links-set (points/create-links nodes links)
        render (render-callback renderer scene camera stats state controls select-circle)]


    ;;     (swap! state assoc :nodes nodes)
    ;;     (swap! state assoc :meshes meshes)
    ;;     (swap! state assoc :links links)
    ;;     (swap! state assoc :links-set links-set)
    (swap! state assoc :classifier classifier)
    (swap! state assoc :select-circle select-circle)


    ;; renderer
    (.setSize renderer width height)
    (.setClearColor renderer 0x404040)

    ;;shadows
    (when (:shadows (:webgl user-map))
      (set! (-> renderer .-shadowMap .-enabled) true)
      (set! (-> renderer .-shadowMap .-type) js/THREE.PCFSoftShadowMap))



    (worker/listen force-worker (fn [event]
                                   (let [message (.-data event)
                                         type (.-type message)
                                         data (.-data message)]
                                     (case type
                                       "ready" (do
                                                 (init-force force-worker dev-mode first-run)
                                                 (events/notify-user-ready chan-out))
                                       "nodes-positions" (let [state @state]
                                                           (points/update-positions! (:nodes state) data)
                                                           ;;(points/update nodeset)
                                                           (points/update-geometry (:links-set state))
                                                           )))))




    ;; if it's not the first time in dev mode
    (when (and (not first-run) dev-mode)
      (tools/fill-window! canvas)
      (.removeEventListener canvas "mousemove")
      (.removeEventListener canvas "click")
      (events/notify-user-ready chan-out))


    ;;(worker/send force-worker "precompute" 50)

    (.add scene select-circle)
		(set! (-> select-circle .-visible) false)

    (.add scene intersect-plane)

    (.addEventListener js/window "resize" (events/onWindowResize canvas renderer camera))



    (let [mouse (events/listen-to-canvas canvas)]
      (events/apply-events-to mouse canvas camera raycaster intersect-plane controls state force-worker chan-out))






    (let [webgl-params (:webgl user-map)
					background? (:background webgl-params)
					lights? (:lights webgl-params)
					spots? (and background? lights?)]
      ;; add background
      (when background?
        (.add scene (tools/get-background)))

      ;; add lights

			(doseq [light (tools/get-lights spots?)]
				(.add scene light)))


    (events/watch-state state :main-watcher)


    (render)

		;; return closures to user

    {:start (start-callback! state render)
     :stop (stop-callback! state)
     :resume (resume-force-callback force-worker)
     :tick (fn [] (worker/send force-worker "tick"))
     :canvas (.-domElement renderer)
     :stats stats
     :nodes (set-nodes-callback state scene)
     :links (set-links-callback state scene)
     :updateForce (update-force-callback state force-worker)

     :force {:size           (fn [array] (worker/send force-worker "size" array))
             :linkStrength   (fn [val] (worker/send force-worker "linkStrength" (worker/serialize val)))
             :friction       (fn [val] (worker/send force-worker "friction" val))
             :linkDistance   (fn [val] (worker/send force-worker "linkDistance" (worker/serialize val)))
             :charge         (fn [val] (worker/send force-worker "charge" (worker/serialize val)))
             :gravity        (fn [val] (worker/send force-worker "gravity" val))
             :theta          (fn [val] (worker/send force-worker "theta" val))
             :alpha          (fn [val] (worker/send force-worker "alpha" val))}

     :selectNode (fn [node]
                    (swap! state assoc :selected node)
                    (:selected @state))
     :pinNode (fn [node]
                 (let [circle (tools/get-circle)
                       mesh (-> node .-mesh)]
                   (set! (-> mesh .-circle) circle)
                   (.add mesh circle))
                 (worker/send force-worker "pin" {:index (-> node .-index)}))
     :unpinNode (fn [node]
                   (tools/remove-children (-> node .-mesh))
                   (.remove (-> node .-mesh) (-> node .-mesh .-circle))
                   (worker/send force-worker "unpin" {:index (-> node .-index)}))
     :camera camera
     }))










