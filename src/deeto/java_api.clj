(ns deeto.java-api
  "Deeto's Java API.

  Actually the functions (or methods) in this namespace could be
  static. There is no context that the factory gives to `newInstance`
  and `copyOf`. But we need an interface to define generic
  typed-methods for Java users and so we need an instance that
  implements this interface."
  
  (:require [deeto.core :as c])
  (:gen-class
   :name deeto.Deeto
   :implements [deeto.IFactory]
   :methods [^:static [factory [] deeto.IFactory]]))

(def the-factory
  (deeto.Deeto.))

(defn -factory
  "Returns the one and only Deeto factory."

  []
  the-factory)

(defn -newInstance
  "Creates and returns a Java dynamic proxy for the given interface
  class."

  [this clazz]
  (c/make-proxy clazz))

(defn -copyOf
  "Serializes `o` and returns the deserialized serial form. Thus it
  returns a deep copy of `o`."

  [this o]
  (c/ser-de-ser o))

  

  


