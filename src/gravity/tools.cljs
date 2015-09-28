(ns gravity.tools)

(defn log 
  "Log in the console"
  [args]
  (.log js/console "[force.worker/log]: " args))

(defn warn 
  "Warn in the console"
  [args]
  (.warn js/console "[force.worker/warn]: " args))