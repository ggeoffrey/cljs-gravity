(ns ^:figwheel-always gravity.core
  (:require [gravity.view.graph :as graph]
            [gravity.view.tools :as tools]
            [gravity.events :as events]
            [gravity.force.proxy :as worker]
            [gravity.tools :refer [log]]
            [gravity.demo :as demo])
  (:require-macros [gravity.macros :refer [λ]]))

(enable-console-print!)

(defonce app-state (atom {}))

(+ 1 2)

(def default-parameters {:color (.category10 js/d3.scale)
                         :force {:size [1 1]
                                 :linkStrength 1
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
    (on "node-over" (λ [node]
                      (set! (-> canvas .-style .-cursor) "pointer")))
    (on "node-blur" (λ []
                      (set! (-> canvas .-style .-cursor) "inherit")))
    (on "node-select" (λ [node]
                        (log [:select (.-name node) node])))
    (on "void-click" (λ []
                       (log [:void])))
    (on "node-click" (λ [node]
                        (log :node-click)
                        (let [select (:selectNode graph)]
                          (select node))))
    (on "node-dbl-click" (λ [node]
                         (log :dbl-click)))
    (on "ready" (λ []
                   (let [set-nodes (:nodes graph)
                         set-links (:links graph)
                         update-force (:updateForce graph)
                         data (demo/get-demo-graph)
                         nodes (-> data .-nodes)
                         links (-> data .-links)]
                     (set-nodes nodes)
                     (set-links links)
                     (update-force)
                     )))))


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
        color (if (:color user-map)
                (:color user-map)
                (:color default-parameters))
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

   (let [chan-out (events/create-chan)
         store (events/create-store)
         graph (graph/create user-map chan-out dev-mode)  ;; <--
         graph (-> graph
                   (merge store)
                   (dissoc :get-callbacks))]
     (events/listen-outgoing-events chan-out store)
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
        dev-app-state {:stats (tools/make-stats)
                       :last-instance {}
                       :first-run true}
        params (init-parameters user-map)
        state (merge dev-app-state user-map params)]
    (reset! app-state state)
    (swap! app-state assoc :force-worker (worker/create "force-worker/worker.js" (:force state)))

    (clj->js
     (on-js-reload))))





