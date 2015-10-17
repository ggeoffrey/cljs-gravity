(ns gravity.view.node)



(defn- get-color   ;; TODO
  "Give a color for a given node"
  [node classifier]
  (let [key (.-group node)]
    (new js/THREE.Color (classifier key))))

(def get-unique-color
  (memoize get-color))


(defn get-rand-pos
  "Give a random position between -extent and +extent"
  [extent]
  (- (rand (* extent 2)) extent))


(defn generate-geometry
  "Generate a generic geometry"
  []
  (new js/THREE.SphereGeometry 10 10 10))


(def get-unique-geometry
  (memoize generate-geometry))

(defn generate-material
  "Generate a generic material"
  [color-key]
  (new js/THREE.MeshLambertMaterial (clj->js {:color (new js/THREE.Color color-key)
                                            :visible true
                                            :wireframe false})))


(def get-unique-material
  (memoize generate-material))


(defn generate-collider
  "create and return a new node mesh used for collisions"
  [node classifier]
  (let [geometry (get-unique-geometry)
        material (get-unique-material (get-unique-color node classifier))
        sphere (new js/THREE.Mesh geometry material)]
    (.set (.-scale sphere) 0.3 0.3 0.3)
    sphere))



(defn create
  "Return a cloned node with a random position and a collider object"
  [node classifier]
  (let [ext 2000
        node (.clone js/goog.object node)
        collider (generate-collider node classifier)
        position (new js/THREE.Vector3 (get-rand-pos ext) (get-rand-pos ext) 0)]
    (set! (.-position node) position)
    (set! (.-mesh node) collider)
    (set! (.-castShadow collider) true)
    (set! (.-node collider) node)
    node))

