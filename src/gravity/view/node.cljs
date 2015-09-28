(ns gravity.view.node)


(defn get-rand-pos 
  "Give a random position between -extent and +extent"
  [extent]
  (- (rand (* extent 2)) extent))



;;(defn create
;;  "create and return a new node sphere"
;;  []
;;  (let [geometry (new js/THREE.BoxGeometry 5 5 5)
;;        material (new js/THREE.MeshBasicMaterial (clj->js {:color 0xff0000}))
;;        sphere (new js/THREE.Mesh geometry material)
;;        extent 1000]
;;    (.set (.-position sphere) (get-rand-pos extent) (get-rand-pos extent) (get-rand-pos extent))
;;    sphere)
;;  )

(defn create
	"Return a node particle position"
	[]
 	(let [ext 2000
 		  node #js {}]
 		(set! (.-position node) (new js/THREE.Vector3 (get-rand-pos ext) (get-rand-pos ext) 0))))



(defn set-color
	"Set the color of a node"
	[node hex-color]
	(.setHex (.-color (.-material node)) hex-color)
	nil)