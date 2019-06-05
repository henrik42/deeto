(ns deeto.java-api
  (:require [deeto.core :as c])
  (:gen-class
   :name deeto.Deeto
   :implements [deeto.Factory]
   :methods [^:static [factory [] deeto.Factory]
             #_ [newInstance [Class] Object]]))

(def the-factory
  (deeto.Deeto.))

(defn -newInstance [this clazz]
  (c/make-proxy clazz))

(defn -factory []
  the-factory)
  

  


