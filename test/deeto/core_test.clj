(ns deeto.core-test
  (:require [clojure.test :refer :all]
            [deeto.core :refer :all]))

;; lein do clean, test

(def string-property ["String" {:property-name "String",
                                :property-type String,
                                :property-getter "getString",
                                :property-setter "setString"
                                :property-mutator "string"}])

(def int-property ["Int" {:property-name "Int",
                          :property-type Integer/TYPE
                          :property-getter "getInt",
                          :property-setter "setInt"
                          :property-mutator nil}])

(def int-array-property ["IntArray" {:property-name "IntArray",
                                     :property-type (-> (into-array Integer/TYPE []) .getClass)
                                     :property-getter "getIntArray",
                                     :property-setter "setIntArray"
                                     :property-mutator nil}])

;; i's class P is a Java dynamic proxy class [6] wich implements I, Cloneable and Serializable.
(deftest test-basics
  (let [i (make-proxy deeto.StringDto)
        c (.getClass i)
        j (ser-de-ser i)
        k (.getClass j)]
    (is (= true (.equals i j)))
    (is (= true (.equals j i)))
    (is (= true (java.lang.reflect.Proxy/isProxyClass c)))
    (is (= true (java.lang.reflect.Proxy/isProxyClass k)))
    (is (= [deeto.StringDto java.io.Serializable java.lang.Cloneable]
           (->> c .getInterfaces (into []))))
    (is (= [deeto.StringDto java.io.Serializable java.lang.Cloneable]
           (->> k .getInterfaces (into []))))))

(deftest test-reflect-on
  (is (= (into {} [string-property])
         (reflect-on deeto.StringDto)))
  (is (= (into {} [int-property int-array-property])
         (reflect-on deeto.intDto))))

(defn *get-method [clazz name]
  (->> clazz .getMethods (filter (fn [m] (= name (.getName m)))) first))

(deftest test-reflect-on-method
  (is (= {:method-name "getString",
          :get-property "String",
          :set-property nil,
          :mutate-property nil,
          :return-type java.lang.String,
          :parameter-types []}
         (reflect-on-method deeto.StringDto (*get-method deeto.StringDto "getString"))))
  (is (= {:method-name "setString",
          :get-property nil,
          :set-property "String",
          :mutate-property nil,
          :return-type java.lang.Void/TYPE,
          :parameter-types [String]}
         (reflect-on-method deeto.StringDto (*get-method deeto.StringDto "setString"))))
  (is (= {:method-name "string",
          :get-property nil,
          :set-property nil
          :mutate-property "String"
          :return-type deeto.StringDto,
          :parameter-types [String]}
         (reflect-on-method deeto.StringDto (*get-method deeto.StringDto "string"))))
)  

(deftest test-make-proxy
  (let [int-dto (make-proxy deeto.intDto)]
    (is (= "{:type deeto.intDto, :value {\"Int\" 0, \"IntArray\" nil}}"
           (.toString int-dto)))))

(deftest test-to-string
  (let [int-dto (make-proxy deeto.intDto)]
    (.setInt int-dto 1)
    (is (= "{:type deeto.intDto, :value {\"Int\" 1, \"IntArray\" nil}}"
           (.toString int-dto)))))

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

(deftest test-clone
  (let [int-array-a (into-array Integer/TYPE [1 2])
        int-dto-a (doto (make-proxy deeto.intDto)
                    (.setIntArray int-array-a))
        int-dto-b (.clone int-dto-a)]
    ;; (println (str int-dto-a int-dto-b))
    (is (.equals int-dto-a int-dto-b))
    (is (.equals int-dto-b int-dto-a))
    (is (= (.hashCode int-dto-a) (.hashCode int-dto-b)))))

(deftest test-hashcode
  (let [double-dto-a (make-proxy deeto.DoubleDto)
        double-dto-b (.clone double-dto-a)]
    
    (is (.equals double-dto-a double-dto-b))
    (is (= (.hashCode double-dto-a) (.hashCode double-dto-b)))
    
    (.nativeDoubleProp double-dto-a -0.0)
    
    (is (.equals double-dto-a double-dto-b))
    (is (= (.hashCode double-dto-a) (.hashCode double-dto-b)))))
    
(deftest test-is
  (is (= {"BooleanProp" {:property-name "BooleanProp",
                         :property-type Boolean/TYPE
                         :property-mutator nil,
                         :property-getter "isBooleanProp",
                         :property-setter nil}}
         (reflect-on deeto.BooleanDto))))

;; Setters should clone argument values. So mutating the argument
;; (e.g. an array) after calling the setter should not change the dto.
(deftest test-setter
  (let [int-array-a (into-array Integer/TYPE [1 2])
        int-array-b (into-array Integer/TYPE [1 2])
        int-dto-a (doto (make-proxy deeto.intDto)
                    (.setIntArray int-array-a))
        int-dto-b (doto (make-proxy deeto.intDto)
                    (.setIntArray int-array-b))]
    
    (is (.equals int-dto-a int-dto-b))
    
    ;; apply mutator to int-array-1. If the setter took a clone/copy
    ;; of the array into its internal state, then this mutation should
    ;; have no side-effect on int-dto-a!!!
    (aset int-array-a 1 3)
    ;; dtos should be equal in the absence of side-effects
    (is (.equals int-dto-a int-dto-b))

    (aset (.getIntArray int-dto-a) 1 3)
    ;; dtos should be equal in the absence of side-effects
    (is (.equals int-dto-a int-dto-b))
    
    ;; calling the setters should make them equal again
    (.setIntArray int-dto-a int-array-a)
    (is (false? (.equals int-dto-a int-dto-b)))
    
    (.setIntArray int-dto-b int-array-a)
    (is (.equals int-dto-a int-dto-b))))

(deftest test-builder
  (let [string-dto (make-proxy deeto.StringDto)]
    (is (= "foo" (-> string-dto (.string "foo") .getString)))))

;; TBD: P's int hashCode() implementation (of int Object.hashCode())
;; is consistent with P's boolean equals(Object) implementation.
(deftest test-hash-code
  (let [int-dto (make-proxy deeto.intDto)]))

(deftest test-double
  (let [double-dto-a (doto (make-proxy deeto.DoubleDto)
                       (.doubleProp 0.0)
                       (.nativeDoubleProp 0.0))
        double-dto-b (doto (make-proxy deeto.DoubleDto)
                       (.doubleProp 0.0)
                       (.nativeDoubleProp -0.0))]
    (is (= double-dto-a double-dto-b))))

