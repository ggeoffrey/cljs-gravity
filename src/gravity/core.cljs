(ns ^:figwheel-always gravity.core
  (:require [gravity.view.graph :as graph]
            [gravity.view.tools :as tools]
            [gravity.events :as events]
            [gravity.force.proxy :as worker]
            [gravity.tools :refer [log]])
  (:require-macros [gravity.macros :refer [λ]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:force-worker nil
                          :canvas nil
                          :stats nil}))



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
    (off)))


(defn ^:export main

  ([user-map]
   (main user-map false))

  ([user-map dev-mode]

   (let [chan (events/create-chan)
         store (events/create-store)
         graph (graph/create user-map chan dev-mode)
         graph (merge graph store)]
     (events/listen chan store)
     (bind-dev-events graph)
     (clj->js graph))))



(defn ^:export init-dev-mode
  "Set some params to use live-reload in dev mode"
  [canvas]
  (swap! app-state assoc-in [:force-worker] (worker/create "force-worker/worker.js"))
  (swap! app-state assoc-in [:stats] (tools/make-stats))
  (swap! app-state assoc-in [:canvas] canvas)

  (let [graph (main @app-state false)]
    (swap! app-state assoc-in [:last-instance] graph)
    graph))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;(swap! app-state update-in [:__figwheel_counter] inc)

  (let [state @app-state
        last-instance (:last-instance state)]
    (when-not (nil? last-instance)
      (unbind-old-events last-instance)
      (.stop last-instance)
      (let [graph (main state true)]
        (swap! app-state assoc-in [:last-instance] graph)
        graph))))

