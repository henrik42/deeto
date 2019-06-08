(ns deeto.java-api
  (:require [deeto.core :as c])
  (:gen-class
   :name deeto.Deeto
   :implements [deeto.IFactory]
   :methods [^:static [factory [] deeto.IFactory]]))

(def the-factory
  (deeto.Deeto.))

(defn -factory []
  the-factory)

(defn -newInstance [this clazz]
  (c/make-proxy clazz))

(defn -copyOf [this o]
  (c/ser-de-ser o))

  

  


