(ns deeto.core)

(defn ser-de-ser
  "Returns a deep clone. The clone is created by serializing and
   deserializing any non-nil argument. Checks that the argument and
   the clone are `Arrays/deepEquals`. Returns `nil` for `nil`."

  [x]
  {:post [(java.util.Arrays/deepEquals (into-array Object [x]) (into-array Object [%]))]}
  (if (nil? x) nil
      (with-open [baos (java.io.ByteArrayOutputStream.)
                  oos (java.io.ObjectOutputStream. baos)]
        (.writeObject oos x)
        (with-open [bais (java.io.ByteArrayInputStream. (.toByteArray baos))
                    ois (java.io.ObjectInputStream. bais)]
          (.readObject ois)))))

(defn reflect-on
  "Inspects via reflection all methods of the DTO `clazz` and
   determines the __properties__ by looking at __getter__ and
   __setter__ methods.

   Throws if any of the properties are __inconsistent__ (e.g. type
   mismatch between getter and setter, wrong numer ob arguments, wrong
   return type)."

  [clazz]
  (reduce
   (fn [res {:keys [method-name get-property set-property return-type parameter-types] :as m}]
     ;; Object.getClass() is not a DTO property-getter! TBD: do we
     ;; need this?
     (if-let [property-name (let [x (or get-property set-property)]
                              (when-not (= "Class" x) x))]
       ;; method is a getter or setter
       (assoc res property-name 
              (if-let [{:keys [property-type property-getter property-setter] :as p} (res property-name)]
                ;; we've seen the property before
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

                ;; seeing the property the first time
                {:property-name property-name
                 :property-type (if set-property (first parameter-types) return-type)
                 :property-getter (when get-property
                                    ;; DRY! This repeats the stuff from aboe
                                    (cond
                                      (or 
                                       (not (empty? parameter-types))
                                       (= java.lang.Void/TYPE return-type))
                                      (throw (ex-info "Not a valid getter" {:res res :m m}))
                                      :else method-name))
                 :property-setter (when set-property
                                    (cond
                                      ;; DRY! This repeats the stuff from aboe
                                      (or 
                                       (not= 1 (count parameter-types))
                                       (not= java.lang.Void/TYPE return-type))
                                      (throw (ex-info "Not a valid setter" {:res res :m m}))
                                      :else method-name))}))
       ;; Method is non-getter/setter (like equals, hashCode, clone,
       ;; readResolve,...) So we ignore it here! We could check that
       ;; it is one of the known other methods that we support.
       (do 
         ;; (println "no getter/setter" m)
         res)))
   (sorted-map) ;; Start value is an empty map
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
  "Returns a Java dynamic proxy (_Deeto proxy_) for the given
   interface(s). Any invocation on the proxy will be delegated to the
   `handler-fn` which should be a function of `the-method` and
   `the-args` of `java.lang.reflect.InvocationHandler/invoke`.

   Calling the overloaded 1-arity variant will create a `handler-fn`
   which keeps an internal state `atom` and delegates calls and the
   state to `handler` (see below)."
  
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

(defn handle-equals
  "Returns `true` if `@state` and the first value of `the-args` are
   equal in terms of `java.util.Arrays/deepEquals`."

  [state the-method the-args]
  (let [the-args (into [] the-args)
        other (first the-args)]
    (cond 
      (not= 1 (count the-args))
      (throw (ex-info "Not a valid equals method" {:the-method the-method :the-args the-args}))
      
      (nil? other)
      false
      
      :else 
      ;; #_
      (let [s1 (into-array Object (vals @state))
            s2 (into-array Object (vals (get-state other)))]
        ;; (println "states=" (into [] s1) (into [] s2))
        (java.util.Arrays/deepEquals s1 s2))
      #_ ;; altenative -- should be removed. 
      (reduce (fn [v [p v1 v2]]
                #_
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
  ;; Special access to @state via null method (see get-state)
  (if-not the-method @state
          (let [method-name (.getName the-method)
                get-property (-> (re-matches #"get(.+)" method-name) second)
                set-property (-> (re-matches #"set(.+)" method-name) second)
                return-type (.getReturnType the-method)
                parameter-types (into [] (.getParameterTypes the-method))]
            (cond
              
              get-property (ser-de-ser (@state get-property))
              set-property (swap! state assoc set-property (ser-de-ser (first the-args)))
              
              (= "toString" method-name) (str @state)
              (= "equals" method-name) (and
                                        ;; classes match?
                                        (.equals clazz nil)
                                        (handle-equals state the-method the-args))
              (= "hashCode" method-name) (java.util.Arrays/deepHashCode (into-array Object (vals @state)))

              ;; Note: for clone we do not need to create a deep copy!
              ;; Since we'll never mutate internal values and we never
              ;; leak references to the outside there is no danger
              ;; that anyone ever changes state. So effectivly we hava
              ;; constant values (which you need not clone).
              (= "clone" method-name)
              (make-proxy clazz (partial #'handler clazz properties (atom @state)))
              
              :else (throw (ex-info "Unknown invocation"
                                    {:properties properties
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