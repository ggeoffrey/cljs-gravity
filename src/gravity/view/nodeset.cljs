(ns gravity.view.nodeset
	 (:require [gravity.tools :as t]))



(defn create
	"Create a new nodeset"
	[]
	(let [geometry (new js/THREE.Geometry)
		  material-params { :size 10
		  					:map (.loadTexture js/THREE.ImageUtils "assets/img/circle.png")
		  					:blending js/THREE.AdditiveBlending
		  					:transparent false
		  					:vertexColors true}
		  material (new js/THREE.PointsMaterial (clj->js material-params))
		  particle-system (new js/THREE.Points geometry material)]
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

(defn prepare-nodes!
  "Add a Vector3 object in each nodes to hold their position."
  [nodes]
  (loop [i 0]
	(let [node (aget nodes i)]
		(set! (.-position node) (new js/THREE.Vector3 )))
	(when (< i (dec (.-length nodes)))
		(recur (inc i))))
  nodes)

(defn add-to
	"Add a node to a nodeset, return new nodset"
	[nodeset node]
	(.push (.-vertices (.-geometry nodeset)) node)
	nodeset)


(defn add-all!
	"Add all nodes to the nodeset, add a position vector3 inside the nodes, return new nodeset"
	[nodeset nodes]
	(let   [geometry (.-geometry nodeset)
			colors (.-colors geometry)
			classifier (.category10 js/d3.scale)]
			(set! (.-colors geometry) #js [])
			(loop [i 0]
				(let [node (aget nodes i)]
					(.push colors (get-color classifier node ))
					(.push (.-vertices geometry) (.-position node)))
				(when (< i (dec (.-length nodes)))
					(recur (inc i))))
			(set! (.-verticesNeedUpdate geometry) true)
			(set! (.-colors geometry) colors)
			(set! (.-colorsNeedUpdate  geometry) true))
	nodeset)


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
             (when (< i size)
               (recur (inc i)))))))


; Colors & shape

(def color-map (atom {}))

(defn get-color   ;; TODO
  "Give a color for a node, return a unique object color for a given color (singleton)"
  [classifier node]
  (let [color-code (classifier (.-group node))
  		cmap @color-map]
  	(if (contains? cmap color-code)
  		(get cmap color-code)
  		;else
  		(do 
  			(let [color (new js/THREE.Color color-code)]
  				(swap! color-map assoc color-code color)
  				color)))))

