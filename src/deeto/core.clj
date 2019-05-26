(ns deeto.core)

(def my-dto-class (Class/forName "deeto.example.MyDto"))

(defn reflect-on
  "Inspects via reflection all methods of the DTO `clazz` and
  determines the _properties_ by looking at getter and setter methods.

  Throws if any of the properties are _inconsistent_ (e.g. type
  mismatch between getter and setter)."

  [clazz]
  (reduce
   (fn [res {:keys [method-name get-property set-property return-type parameter-types] :as m}]
     ;; Object.getClass() is not a DTO property! TBD: do we need this?
     (if-let [property-name (let [x (or get-property set-property)]
                              (when-not (= "Class" x) x))]
       ;; method is a getter or setter
       (assoc res property-name 
              (if-let [{:keys [property-type property-getter property-setter] :as p} (res property-name)]
                (assoc p
                  :property-getter
                  (if-not get-property property-getter
                          (cond
                            property-getter
                            (throw (ex-info "Duplicate getter" {:res res :m m}))
                            (not= property-type return-type)
                            (throw (ex-info "Getter's type mismatch" {:res res :m m}))
                            (or 
                             (not (empty? parameter-types))
                             (= java.lang.Void/TYPE return-type))
                            (throw (ex-info "Not a valid getter" {:res res :m m}))
                            :else method-name))
                  :property-setter
                  (if-not set-property property-setter
                          (cond
                            property-setter
                            (throw (ex-info "Duplicate setter" {:res res :m m}))
                            (not= property-type (first parameter-types))
                            (throw (ex-info "Setter's type mismatch" {:res res :m m}))
                            (or 
                             (not= 1 (count parameter-types))
                             (not= java.lang.Void/TYPE return-type))
                            (throw (ex-info "Not a valid setter" {:res res :m m}))
                            :else method-name)))
                {:property-name property-name
                 :property-type (if set-property (first parameter-types) return-type)
                 :property-getter (when get-property
                                    (cond
                                      (or 
                                       (not (empty? parameter-types))
                                       (= java.lang.Void/TYPE return-type))
                                      (throw (ex-info "Not a valid getter" {:res res :m m}))
                                      :else method-name))
                 :property-setter (when set-property
                                    (cond
                                      (or 
                                       (not= 1 (count parameter-types))
                                       (not= java.lang.Void/TYPE return-type))
                                      (throw (ex-info "Not a valid setter" {:res res :m m}))
                                      :else method-name))}))
       ;; Method is non-getter/setter (like equals, hashCode, clone, readResolve,...)
       (do
         (println m)
         res)))
   {} ;; Start value is an empty map
   (map (fn [m] ;; seq of method-map containing preprocessed things about each method
          (let [n (.getName m)]
            {:method-name n
             :get-property (-> (re-matches #"get(.+)" n) second)
             :set-property (-> (re-matches #"set(.+)" n) second)
             :return-type (.getReturnType m)
             :parameter-types (into [] (.getParameterTypes m))}))
        (.getMethods clazz))))

(defn make-proxy
  [proxy-type handler-fn]
  (let [pt (if (vector? proxy-type)
             (into-array proxy-type)
             (into-array [proxy-type]))]
    (java.lang.reflect.Proxy/newProxyInstance
     (.getClassLoader (first pt)) 
     pt
     (proxy [java.lang.reflect.InvocationHandler] []
       (invoke [the-proxy the-method the-args]
         (handler-fn the-method the-args))))))

(defprotocol Factory
  (newInstance [this]))

(defn factory-for [class]
  (reify Factory
    (newInstance [this]
      nil)))