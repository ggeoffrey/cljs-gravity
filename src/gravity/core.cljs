(ns ^:figwheel-always gravity.core
  (:require [gravity.view.graph :as graph]
            [gravity.force.proxy :as worker]
            [gravity.tools :as t]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:canvas nil
                          :force-worker nil
                          :stats nil
                          :camera nil
                          :camera-pos nil}))



(defn ^:export main
  [user-map dev-mode]
  (graph/create (js->clj user-map  :keywordize-keys true) dev-mode))



(defn ^:export init-dev-mode
  "Set some params to use live-reload in dev mode"
  [canvas]
  (swap! app-state assoc-in [:force-worker] (worker/create "force-worker/worker.js"))
  (swap! app-state assoc-in [:stats] (t/make-stats))
  (swap! app-state assoc-in [:canvas] canvas)

  (let [graph (main @app-state false)]
    (reset! app-state graph)
    (clj->js graph))
  )


(defn on-js-reload []
  ((:stop @app-state))
  (swap! app-state assoc :camera-pos (.clone (.-position (:camera @app-state))))

  (let [graph (main @app-state true)]
    (clj->js graph))
  )

