(ns deeto.core)

(defn ser-de-ser [x]
  {:post [(java.util.Arrays/deepEquals (into-array Object [x]) (into-array Object [%]))]}
  (if (nil? x) nil
      (let [baos (java.io.ByteArrayOutputStream.)
            oos (java.io.ObjectOutputStream. baos)]
        (.writeObject oos x)
        (.readObject (java.io.ObjectInputStream. (java.io.ByteArrayInputStream. (.toByteArray baos)))))))

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
         (println "no getter/setter" m)
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

(declare handler)

(defn make-proxy
  ([proxy-type]
     (let [properties (reflect-on proxy-type)
           state (atom (into (sorted-map)
                             (map #(-> [% nil]) (keys properties))))]
       (make-proxy proxy-type
                   (partial #'handler proxy-type properties state))))
  ([proxy-type handler-fn]
     (let [pt (if (vector? proxy-type)
                (into-array proxy-type)
                (into-array [proxy-type]))]
       (java.lang.reflect.Proxy/newProxyInstance
        (.getClassLoader (first pt)) 
        pt
        (proxy [java.lang.reflect.InvocationHandler] []
          (invoke [the-proxy the-method the-args]
            (handler-fn the-method the-args)))))))

(defn get-state
  "Returns internal state (map) of Deeto proxy."

  [p]
  (.invoke (java.lang.reflect.Proxy/getInvocationHandler p)
           nil nil nil))

(defn handle-equals [state the-method the-args]
  (let [the-args (into [] the-args)
        other (first the-args)]
    (cond 
      (not= 1 (count the-args)) (throw (ex-info "Not a valid equals method" {:the-method the-method :the-args the-args}))
      (nil? other) false
      :else 
      #_
      (let [s1 (into-array Object (seq @state))
            s2 (into-array Object (seq (get-state other)))]
        (println "states=" (into [] s1) (into [] s2))
        (java.util.Arrays/deepEquals s1 s2))
      ;; #_
      (reduce (fn [v [p v1 v2]]
                {:pre [(or (println "compare" [p v1 v2]) true)]
                 :post [(or (println "compare" [p v1 v2] " -> " %) true)]}
                (if-not 
                    (java.util.Arrays/deepEquals (into-array Object [v1]) (into-array Object [v2]))
                  (reduced false)
                  true))
              true 
              (map (fn [[p1 v1] [p2 v2]]
                     [p1 v1 v2])
                   @state (get-state other))))))

(defn handler [clazz properties state the-method the-args]
  ;; (println "call" the-method the-args)
  ;; Special access to @state via null method (see get-state)
  (if-not the-method @state
          (let [method-name (.getName the-method)
                get-property (-> (re-matches #"get(.+)" method-name) second)
                set-property (-> (re-matches #"set(.+)" method-name) second)
                return-type (.getReturnType the-method)
                parameter-types (into [] (.getParameterTypes the-method))]
            (cond
              
              ;; TBD: return clone!
              get-property (ser-de-ser (@state get-property))
              
              set-property (swap! state assoc set-property (ser-de-ser (first the-args)))
              
              (= "toString" method-name) (str @state)
              (= "equals" method-name) (handle-equals state the-method the-args)
              (= "hashCode" method-name) (java.util.Arrays/deepHashCode (into-array Object (vals @state)))

              ;; TBD: create clone of vals
              (= "clone" method-name)
              (make-proxy clazz (partial #'handler clazz properties (atom @state)))
              
              :else (throw (ex-info "oops" {:properties properties
                                            :state state
                                            :the-method the-method
                                            :the-args (into [] the-args)}))))))


#_
(defprotocol Factory
  (newInstance [this]))

#_
(defn factory-for [class]
  (reify Factory
    (newInstance [this]
      nil)))