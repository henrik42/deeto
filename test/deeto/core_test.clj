(ns deeto.core-test
  (:require [clojure.test :refer :all]
            [deeto.core :refer :all]))

;; lein do clean, test

(def string-property ["String" {:property-name "String",
                                :property-type String,
                                :property-getter "getString",
                                :property-setter "setString"}])

(def int-property ["Int" {:property-name "Int",
                          :property-type Integer/TYPE
                          :property-getter "getInt",
                          :property-setter "setInt"}])

(def int-array-property ["IntArray" {:property-name "IntArray",
                                     :property-type (-> (into-array Integer/TYPE []) .getClass)
                                     :property-getter "getIntArray",
                                     :property-setter "setIntArray"}])


;; i's class P is a Java dynamic proxy class [6] wich implements I, Cloneable and Serializable.
(deftest test-basics
  (let [i (make-proxy deeto.StringDto)
        c (.getClass i)]
    (is (= true (java.lang.reflect.Proxy/isProxyClass c)))
    (is (= [deeto.StringDto java.io.Serializable java.lang.Cloneable]
           (->> c .getInterfaces (into []))))))

;;(def i (make-proxy deeto.StringDto))

(deftest test-reflect-on
  (is (= (into {} [string-property])
         (reflect-on deeto.StringDto)))
  (is (= (into {} [int-property int-array-property])
         (reflect-on deeto.intDto))))

(deftest test-make-proxy
  (let [int-dto (make-proxy deeto.intDto)]
    (is (= "{\"Int\" nil, \"IntArray\" nil}" (.toString int-dto)))))

(deftest test-to-string
  (let [int-dto (make-proxy deeto.intDto)]
    (.setInt int-dto 1)
    (is (= "{\"Int\" 1, \"IntArray\" nil}" (.toString int-dto)))))

;; Getters should return clones of internal state values. So mutating
;; the return value (e.g. an array) after calling the getter should
;; not change the dto.
(deftest test-getter
  (let [int-dto (make-proxy deeto.intDto)
        int-dto-b (make-proxy deeto.intDto)]
    (.setInt int-dto 1)
    (.setIntArray int-dto (into-array Integer/TYPE [1 2]))
    (.setIntArray int-dto-b (into-array Integer/TYPE [1 2]))
    (.setInt int-dto-b 1)
    (is (= 1 (.getInt int-dto)))
    (is (= true (.equals int-dto int-dto-b)))
    ;; if the getter returns a clone then mutating the returned array
    ;; should have no side-effect on int-dto
    (aset (.getIntArray int-dto) 1 3)
    (is (= true (.equals int-dto int-dto-b)))))

;; TBD: P's boolean equals(Object o) implementation (of boolean
;; Object.equals(Object)) returns true if o and i are of the same
;; Class and if all properties are equals(Object)
(deftest test-equals
  (let [int-dto-a (doto (make-proxy deeto.intDto)
                    (.setInt 1))
        int-dto-b (doto (make-proxy deeto.intDto)
                    (.setInt 1))]
    (is (= false (.equals int-dto-a nil))) ;; test against nil/null
    (is (= true ;; test identity
           (.equals int-dto-a int-dto-a)))
    (is (= true ;; test equality
           (.equals int-dto-a int-dto-b)))
    (.setIntArray int-dto-a (into-array Integer/TYPE [1 2]))
    (is (= false (.equals int-dto-a int-dto-b)))
    (.setIntArray int-dto-b (into-array Integer/TYPE [1 2]))
    (is (= true (.equals int-dto-a int-dto-b)))))

;; Setters should clone argument values. So mutating the argument
;; (e.g. an array) after calling the setter should not change the dto.
(deftest test-setter
  (let [int-array-a (into-array Integer/TYPE [1 2])
        int-array-b (into-array Integer/TYPE [1 2])
        int-dto-a (doto (make-proxy deeto.intDto)
                    (.setIntArray int-array-a))
        int-dto-b (doto (make-proxy deeto.intDto)
                    (.setIntArray int-array-b))]
    (is (= true (.equals int-dto-a int-dto-b)))
    ;; apply mutator to int-array-1. If the setter took a clone/copy
    ;; of the array into its internal state, then this mutation should
    ;; have no side-effect on int-dto-a!!!
    (aset int-array-a 1 3)
    ;; dtos should be equal in the absence of side-effects
    (is (= true (.equals int-dto-a int-dto-b)))
    ;; calling the setters should make them equal again
    (.setIntArray int-dto-a int-array-a)
    (is (= false (.equals int-dto-a int-dto-b)))
    (.setIntArray int-dto-b int-array-a)
    (is (= true (.equals int-dto-a int-dto-b)))))

;; TBD: P's int hashCode() implementation (of int Object.hashCode())
;; is consistent with P's boolean equals(Object) implementation.
(deftest test-hash-code
  (let [int-dto (make-proxy deeto.intDto)]))

