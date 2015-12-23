(ns gravity.view.tools
  "Contain tools like selection animation, lights, background, etc…"

  (:require [gravity.tools :refer [log]]))



(defn fill-window!
  "Resize a canvas to make it fill the window"
  [canvas]
  (set! (-> canvas .-style .-width) "100%")
  (set! (-> canvas .-style .-height) "100%")
  (let [width (.-offsetWidth canvas)
        height (.-offsetHeight canvas)]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height))
  nil)



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




(defn- get-background
  "Generate a gray sphere as a background"
  []
  (let [material (new js/THREE.MeshLambertMaterial #js {:color 0xa0a0a0
                                                       ;:ambiant  0xffffff
                                                       :side 1})
        geometry (new js/THREE.SphereGeometry 20 20 20)
        background (new js/THREE.Mesh geometry material)]
    (.set (.-scale background) 100 100 100)
    (set! (.-receiveShadow background) true)
    background))



(defn- get-flat-light
  "Generate an ambient light"
  []
  (new js/THREE.AmbientLight 0xffffff))


(defn- get-spot-lights
  []
  (let [color (new js/THREE.Color 0xffffff)
        strength   0.8
        shadow-map 2048
        positions [[-1000 1000 1000]
                   [1000 1000 1000]
                   [-1000 -1000 1000]
                   [1000 -1000 1000]

                   [1000 1000 -1000]]
        lights (map (fn [pos]
                       (let [light (new js/THREE.SpotLight color strength)
                             [x y z] pos]
                         (.set (.-position light) x y z)
                         (set! (.-shadowCameraFar light) 4000)

                         light))
                    positions)]
    (let [main-light (first lights)]
      (set! (.-castShadow main-light) true)
      (set! (.-shadowCameraVisible main-light) true))

    lights))


(defn get-lights
  "Make light(s) for the scene. If spots is true, generate directional lights"
  [spots]
  (if-not spots
    (conj [] (get-flat-light))
    (get-spot-lights)))



(defn get-circle
  "Return a circle meant to be placed and animated on a node."
  ([]
   (get-circle 32 15))

  ([nb-segments radius]
   (let [material (new js/THREE.LineBasicMaterial #js {:color 0xff0000})
         geometry (new js/THREE.Geometry)]
     (doseq [i (range nb-segments)]
       (let [theta (-> i
                       (/ nb-segments)
                       (* Math/PI)
                       (* 2))
             cos (-> (Math/cos theta)
                     (* radius))
             sin (-> (Math/sin theta)
                     (* radius))
             vect (new js/THREE.Vector3 cos sin 0)]
         (.push (-> geometry .-vertices) vect)))
     ;; close circle
     (.push (-> geometry .-vertices) (aget (-> geometry .-vertices) 0))
     ;ret
     (new js/THREE.Line geometry material))))





(defn get-intersect-plane
  "Return a big plane filling the sphere. Used to drag nodes"
  []
  (let [side 4000
        material (new js/THREE.MeshBasicMaterial #js {:wireframe true
																											:visible false})
        geometry (new js/THREE.PlaneGeometry side side 1 1)
        mesh (new js/THREE.Mesh geometry material)]
    mesh))





(defn remove-children
  [object3D]
  (loop [len (dec (-> object3D .-children .-length))
         i (range 0 len)]

    (.remove object3D (aget (-> object3D .-children) 0))

    (when (< i len)
      (recur len (inc i))))
  nil)
