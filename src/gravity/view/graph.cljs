(ns gravity.view.graph
	(:require   [gravity.tools :as t]
		[gravity.view.node :as node]
		[gravity.view.nodeset :as points]
		[gravity.force.proxy :as worker]
		[gravity.demo :as demo]))



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

(defn create
	"Initialise a context in the specified element id"
	[]
	(let [scene (new js/THREE.Scene)
		width (/ (.-innerWidth js/window) 1)
		height (/ (.-innerHeight js/window) 1)
		camera (new js/THREE.PerspectiveCamera 75 (/ width height) 0.1 100000 )
		stats (make-stats)
		controls (new js/THREE.OrbitControls camera)
		renderer (new js/THREE.WebGLRenderer #js {"antialias" true})
  		canvas (.-domElement renderer)
    	raycaster (new THREE.Raycaster)
		classifier (.category10 js/d3.scale)
		data-test (demo/get-demo-graph)
		  ;gen-nodes (clj->js (mapv #(js-obj) (range 0 2000)))
		  {nodes :nodes
		  	colliders :colliders} (points/prepare-nodes (.-nodes data-test))

		links (.-links data-test)
		nodeset (points/create nodes classifier)
		links-set (points/create-links nodes links)
		force-worker (worker/create "force-worker/worker.js")
		state (atom {:should-run true})
		render (render-callback renderer scene camera stats state)]


		(.setSize renderer width height)
		(.setClearColor renderer 0x000000)
		(set! (.-z (.-position camera))  250)
  
		(worker/listen force-worker (fn [event]
			(let [message (.-data event)
				type (.-type message)
				data (.-data message)]
				(case type
					"nodes-positions" (do (points/update-positions! nodes data)
						(points/update nodeset)
						(points/update links-set))))))


	;(worker/send force-worker "select-mode" "2D")

	
	(worker/send force-worker "set-nodes" (.-length nodes))
	(worker/send force-worker "set-links" links)
	(worker/send force-worker "start")

	;(worker/send force-worker "precompute" 50)
	

	(.add scene nodeset)
 	
	(loop [i 0]
		(.add scene (aget colliders i))
		(when (< i (dec (.-length colliders)))
			(recur (inc i))))

	;(.add scene colliders)
	(.add scene links-set)
 
 	(.addEventListener canvas "mousemove" (onDocMouseMove canvas camera raycaster colliders))

 	(render)
  
	(clj->js {:start (start-callback! state render)
		:stop (stop-callback! state)
		:resume (resume-force-callback force-worker)
		:canvas (.-domElement renderer)
		:scene scene
		:stats (.-domElement stats)})

	))


(defn- render-callback
	"Return a function rendering the context"
	[renderer scene camera stats state]
	(fn render []
		(when-not (nil? stats)
			(.begin stats))

		(if (get @state :should-run)
			(do 
     			(.requestAnimationFrame js/window render 50)
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
	      ;(t/log x y)
       	  (.set mouse-pos x y 1)
       	  (.unproject mouse-pos camera)
          ;(t/log mouse-pos)
          ;(t/log (.normalize (.sub mouse-pos cam-position)))
          (.set raycaster cam-position (.normalize (.sub mouse-pos cam-position)))
          (let [item (aget (.intersectObjects raycaster colliders) 0)]
            (when-not (nil? item)
              (t/log (-> item (.-object) (.-node))))))
     false)))

(defn get-targets
  "Cast a ray to intersect objects under the mouse pointer.
  Return the first intersected or nil"
  []
  )
