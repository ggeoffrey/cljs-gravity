(ns gravity.view.node)


(defn get-rand-pos 
  "Give a random position between -extent and +extent"
  [extent]
  (- (rand (* extent 2)) extent))


(defn generate-geometry
  "Generate a generic geometry"
  []
  (new js/THREE.SphereGeometry 5 5 5))


(def get-unique-geometry
  (memoize generate-geometry))

(defn generate-material
  "Generate a generic material"
  []
  (new js/THREE.MeshBasicMaterial (clj->js {:color 0xff0000
                                            :visible false
                                            :wireframe true})))


(def get-unique-material
  (memoize generate-material))


(defn generate-collider
  "create and return a new node mesh used for collisions"
  []
  (let [geometry (get-unique-geometry)
        material (get-unique-material)
        sphere (new js/THREE.Mesh geometry material)]
    sphere))



(defn create
	"Return a cloned node with a random position and a collider object"
	[node]
 	(let [ext 2000
 		  node (.clone js/goog.object node)
     	  collider (generate-collider)
          position (new js/THREE.Vector3 (get-rand-pos ext) (get-rand-pos ext) 0)]
 		(set! (.-position node) position)
   		(set! (.-collider node) collider)
     	(set! (.-node collider) node)
 		node))



(defn set-color
	"Set the color of a node"
	[node hex-color]
	(.setHex (.-color (.-material node)) hex-color)
	nil)