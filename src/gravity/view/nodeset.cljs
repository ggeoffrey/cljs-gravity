(ns gravity.view.nodeset
  (:refer-clojure :exclude [update])
  (:require [gravity.tools :as t]
            [gravity.view.node :as node])
  (:require-macros [gravity.macros :refer [λ]]))


(declare get-unique-color)
(declare get-shader-material)




(defn create-links
  "Given a js-array of nodes and a js array of links, will return a THREE.LineSegments"
  [nodes links]
  (let [geometry (new js/THREE.Geometry)
        vertices (.-vertices geometry)
        material (new js/THREE.LineBasicMaterial #js {"color" 0xffffff})
        system (new js/THREE.LineSegments geometry material)
        size (dec (.-length links))]
    (doseq [link links]
      (let [source (get nodes (.-source link))
            target (get nodes (.-target link))]
        (.push vertices (.-position source))
        (.push vertices (.-position target))))
    (set! (.-verticesNeedUpdate geometry) true)
    (set! (.-castShadow system) true)
    system))

(defn prepare-nodes
  "Create a array of cloned nodes containing a position and a collider object.
  Return a map {nodes[] colliders[]} meant to be destructured.
  The nodes and the colliders are in the same order and share the same position Vector3."
  [nodes classifier]
  (let [pairs (map (λ [node]
                      (let [prepared-node (node/create node classifier)
                            mesh (.-mesh prepared-node)]
                        [prepared-node mesh]))
                   nodes)]
    {:nodes (clj->js (mapv first pairs))
     :meshes (clj->js (mapv last pairs))}))


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
            node (get nodes i)
            x (aget positions j)
            y (aget positions (+ j 1))
            z (aget positions (+ j 2))]
        (.set (-> node .-position) x y z)
        (.set (-> node .-mesh .-position) x y z)
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


;; (declare get-vertex-shader)
;; (declare get-frag-shader)


;; (defn- get-shader-material
;;   "Generate a shader material for a sprite"
;;   [url]
;;   (let [texture (.loadTexture js/THREE.ImageUtils url)
;;         material-params (clj->js {:uniforms { :texture {:type "t"
;;                                                         :value texture}
;;                                               :color {:type "c"
;;                                                       :value (new js/THREE.Color 0xff0000)}}
;;                                   :vertexShader (get-vertex-shader)
;;                                   :fragmentShader (get-frag-shader)
;;                                   :blending js/THREE.AdditiveBlending})
;;         material (new js/THREE.ShaderMaterial material-params)]
;;     material))




;; (defn- get-vertex-shader
;;   []
;;   "
;;   /**
;;   * Multiply each vertex by the
;;   * model-view matrix and the
;;   * projection matrix (both provided
;;   * by Three.js) to get a final
;;   * vertex position
;;   */

;;   uniform vec3 color;
;;   uniform sampler2D texture;

;;   void main() {
;;   vec4 mvPosition = modelViewMatrix * vec4( position, 1.0 );
;;   gl_PointSize = 5.0 * (1.0+ 300.0 / length( mvPosition.xyz ) );
;;   gl_Position = projectionMatrix *
;;   modelViewMatrix *
;;   vec4(position,1.0);
;;   }

;;   ")

;; (defn get-frag-shader
;;   []
;;   "

;;   uniform vec3 color;
;;   uniform sampler2D texture;

;;   vec4 vColor;

;;   void main() {
;;   //vColor = vec4( color, 0.0 );

;;   //if(vColor.a < 0.5 ) discard;

;;   gl_FragColor =  texture2D( texture, gl_PointCoord );
;;   }
;;   ")
