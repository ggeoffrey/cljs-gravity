(ns ^:figwheel-always gravity.core
  (:require [gravity.view.graph :as graph]
            [gravity.view.tools :as tools]
            [gravity.events :as events]
            [gravity.force.proxy :as worker]
            [gravity.tools :refer [log]])
  (:require-macros [gravity.macros :refer [λ]]))

(enable-console-print!)

(defonce app-state (atom {}))



(def default-parameters {:color (.category10 js/d3.scale)
                         :force {:size [1 1]
                                 :linkStrength 0.1
                                 :friction 0.9
                                 :linkDistance 20
                                 :charge -30
                                 :gravity 0.1
                                 :theta 0.8
                                 :alpha 0.1}
                         :webgl {:antialias true
                                 :background true
                                 :lights true
                                 :shadows true}})



(defn bind-dev-events
  [graph]
  (let [{on :on
         canvas :canvas} graph]
    (on "nodeover" (λ [node]
                      (set! (-> canvas .-style .-cursor) "pointer")))
    (on "nodeblur" (λ []
                      (set! (-> canvas .-style .-cursor) "inherit")))
    (on "nodeselect" (λ [node]
                        (log [:select (.-name node) node])))))


(defn unbind-old-events
  [last-instance]
  (let [off (-> last-instance .-off)]
    (when-not (nil? off)
      (off))))



(defn init-parameters
  [user-map]
  (let [user-map (js->clj user-map :keywordize-keys true)
        webgl-params (:webgl user-map)
        force-params (:force user-map)
        color (:color user-map)
        force-merged (merge (:force default-parameters) force-params)
        webgl-merged (merge (:webgl default-parameters) webgl-params)]
    {:color color
     :force force-merged
     :webgl webgl-merged}))


(defn ^:export main

  ([user-map]
   (let [graph (main user-map false)]
     (clj->js graph)))

  ([user-map dev-mode]

   (let [chan (events/create-chan)
         store (events/create-store)
         graph (graph/create user-map chan dev-mode)
         graph (merge graph store)]
     (events/listen chan store)
     (bind-dev-events graph)
     graph)))




(defn on-js-reload
  ([]
   (let [state @app-state
         last-instance (:last-instance state)]

     (when-not (or (nil? last-instance) (empty? last-instance))
       (unbind-old-events last-instance)
       (apply (:stop last-instance) []))


     (let [graph (main state true)]
       (swap! app-state assoc-in [:last-instance] graph)
       (swap! app-state assoc :first-run false)

       graph))))




(defn ^:export init-dev-mode
  "Set some params to use live-reload in dev mode"
  [user-map]
  (let [user-map (js->clj user-map :keywordize-keys true)
        dev-app-state {:force-worker (worker/create "force-worker/worker.js")
                       :stats (tools/make-stats)
                       :last-instance {}
                       :first-run true}
        params (init-parameters user-map)
        state (merge dev-app-state user-map params)]
    (reset! app-state state)

    (clj->js
     (on-js-reload))))




