(ns gravity.macros)



(defmacro λ
  "Simple alias to clojure.core/fn"
  [args & body]
  `(fn ~args ~@body))
