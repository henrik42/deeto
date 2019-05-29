(ns deeto.java-api
  (:gen-class
   :init init
   ;; :state state
   :name deeto.Deeto)
  #_
  (:gen-interface
   :name deeto.Factory
   :methods [[make [Class] Object]]))

(defn -init [&_])


