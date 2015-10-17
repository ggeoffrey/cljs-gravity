(ns ^:figwheel-always gravity.core
    (:require [gravity.view.graph :as graph]
              [gravity.view.tools :as tools]
              [gravity.events :as events]
              [gravity.force.proxy :as worker]
              [gravity.tools :refer [log]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:force-worker nil
                          :canvas nil
                          :stats nil}))



(defn ^:export main

  ([user-map]
   (main user-map false))

  ([user-map dev-mode]

   (let [chan (events/create-chan)
         store (events/create-store)
         graph (graph/create user-map chan dev-mode)
         graph (assoc graph :on (:on store))]
     (events/listen chan store)
     (clj->js graph)))

  )



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

  (when-not (nil? (:last-instance @app-state))
    (.stop (:last-instance @app-state))
    (let [graph (main @app-state true)]
      (swap! app-state assoc-in [:last-instance] graph)
      graph)))

