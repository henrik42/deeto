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

(deftest test-reflect-on
  (is (= (into {} [string-property])
         (reflect-on deeto.StringDto)))
  (is (= (into {} [int-property int-array-property])
         (reflect-on deeto.intDto))))

(deftest test-make-proxy
  (let [int-dto (make-proxy deeto.intDto)]
    (is (= "{\"Int\" nil, \"IntArray\" nil}" (.toString int-dto)))
    (.setInt int-dto 1)
    (is (= "{\"Int\" 1, \"IntArray\" nil}" (.toString int-dto)))))
    