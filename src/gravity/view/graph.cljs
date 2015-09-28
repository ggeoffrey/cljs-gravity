(ns gravity.view.graph
    (:require   [gravity.view.node :as node]
                [gravity.view.nodeset :as points]
                [gravity.force.proxy :as worker]
                [gravity.demo :as demo]))


(defn- log
	[item]
	(.log js/console (clj->js item)))

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
        data-test (demo/get-demo-graph)
          ;nodes (clj->js (mapv node/create (range 0 1000)))
          nodes (points/prepare-nodes! (.-nodes data-test))
          links (.-links data-test)
        nodeset (points/create)
        links-set (points/create-links nodes links)
        force-worker (worker/create "force-worker/worker.js")
        state (atom {:should-run true})
        render (fn cb []
                    (.begin stats)
                    (if (get @state :should-run)
                        (do 
                            (.render renderer scene camera)))
                    (.end stats)
                )]
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

    
	(worker/send force-worker "set-nodes" nodes)
    (worker/send force-worker "set-links" links)
    (worker/send force-worker "start")
	    
	;(worker/send force-worker "precompute" 50)
    
    
    (points/add-all! nodeset nodes)

    (.add scene nodeset)
    (.add scene links-set)



    ((fn forever-render []
      (.requestAnimationFrame js/window forever-render 50)
      (render)))
    


    (clj->js {:start (fn [] 
                (swap! state assoc :should-run true)
                (render))
              :stop (fn [] (swap! state assoc :should-run false) nil)
              :canvas (.-domElement renderer)
              :scene scene
              :stats (.-domElement stats)}
            )
    )
)