(ns gravity.view.nodeset
	 (:require 	[gravity.tools :as t]
            	[gravity.view.node :as node]))



(defn create
	"Create a new Points (formerly ParticleSystem) gives a set of nodes and a color classifier"
	[nodes classifier]
	(let [geometry (new js/THREE.Geometry)
		  material-params { :size 10
		  					:map (.loadTexture js/THREE.ImageUtils "assets/img/circle.png")
		  					:blending js/THREE.AdditiveBlending
		  					:transparent false
		  					:vertexColors true}
		  material (new js/THREE.PointsMaterial (clj->js material-params))
		  particle-system (new js/THREE.Points geometry material)
		  colors (.-colors geometry)]

		  (set! (.-colors geometry) #js [])
		  (loop [i 0]
		  	(let [node (aget nodes i)]
		  		(.push colors (get-unique-color classifier node ))
		  		(.push (.-vertices geometry) (.-position node)))
		  	(when (< i (dec (.-length nodes)))
		  		(recur (inc i))))
		  (set! (.-verticesNeedUpdate geometry) true)
		  (set! (.-colors geometry) colors)
		  (set! (.-colorsNeedUpdate  geometry) true)

		particle-system))



(defn create-links 
  "Given a js-array of nodes and a js array of links, will return a THREE.LineSegments"
  [nodes links]
  (let [geometry (new js/THREE.Geometry)
  		vertices (.-vertices geometry)
  		material (new js/THREE.LineBasicMaterial #js {"color" 0xffffff})
  		system (new js/THREE.LineSegments geometry material)
  		size (dec (.-length links))]
  		(loop [i 0]
  			(let [link (aget links i)
  				  source (aget nodes (.-source link))
  				  target (aget nodes (.-target link))]
  				  (.push vertices (.-position source))
  				  (.push vertices (.-position target)))
  			(when (< i size)
  				(recur (inc i))))
  		(set! (.-verticesNeedUpdate geometry) true)
  		system))

(defn prepare-nodes
  "Create a array of cloned nodes containing a position and a collider object.
  Return a map {nodes[] colliders[]} meant to be destructured.
  The nodes and the colliders are in the same order and share the same position Vector3.
  Complexity is O(n)."
  [nodes]
  (let [clone-arr (array)
        colliders-arr (array)]
    (loop [i 0]
		(let [node (aget nodes i)
        	  prepared-node (node/create node)]
			(.push clone-arr prepared-node)
   			(.push colliders-arr (.-collider prepared-node)))
		(when (< i (dec (.-length nodes)))
			(recur (inc i))))
    {:nodes clone-arr
     :colliders colliders-arr}))


(defn update
	"Update a nodeset Points geometry or a LineSegments geometry"
	[geom-based-item]
	(set! (.-verticesNeedUpdate (.-geometry geom-based-item)) true)
	geom-based-item)


(defn update-positions!
	"Update the nodes' positions according to the raw array given by the force"
	[nodes positions]
 	(let [size (dec (.-length nodes))]
    	(loop [i 0]
       		(let [j (* i 3)
               	  node (aget nodes i)
                  x (aget positions j)
                  y (aget positions (+ j 1))
                  z (aget positions (+ j 2))]
            	(.set (.-position node) x y z)
             	(.set (.-position (.-collider node)) x y z)
             (when (< i size)
               (recur (inc i)))))))


; Colors & shape


(defn get-color   ;; TODO
  "Give a color for a given node"
  [classifier node]
  (let [key (.-group node)]
    (new js/THREE.Color (classifier key))))

(def get-unique-color
  (memoize get-color))