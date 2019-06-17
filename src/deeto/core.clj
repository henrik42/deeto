(ns deeto.core
  (:gen-class
   :name deeto.SerializableInvocationHandler
   :constructors {[Object] []}
   :init init
   :state state
   :implements [deeto.ISerializable java.lang.reflect.InvocationHandler]))

(defn object-info
  "Returns a map with some info about the argument. Used for
  trouble-shooting."

  [x]
  {:to-string (str x)
   :class (when-not (nil? x) (.getClass x))
   :interfaces (when-not (nil? x) (->> x .getClass .getInterfaces (into [])))})

(defn ser-de-ser
  "Returns a deep clone. The clone is created by serializing and
   deserializing any non-nil argument. Checks that the argument and
   the clone are `Arrays/deepEquals`. Returns `nil` for `nil`."

  [x]
  {:post [(or (java.util.Arrays/deepEquals (into-array Object [x])
                                           (into-array Object [%]))
              (throw (ex-info "Copy is not equal to original!"
                              {:original (object-info x)
                               :copy (object-info %)})))]}
  (try 
    (if (nil? x) nil
        (with-open [baos (java.io.ByteArrayOutputStream.)
                    oos (java.io.ObjectOutputStream. baos)]
          (.writeObject oos x)
          (with-open [bais (java.io.ByteArrayInputStream. (.toByteArray baos))
                      ois (java.io.ObjectInputStream. bais)]
            (.readObject ois))))
    (catch Throwable t
      (throw (ex-info (str "Creating copy failed: " t) (object-info x) t)))))

(defn capitalize
  "Returns string argument with the first character converted to
   upper-case (Locale/ENGLISH). Note that `clojure.str/capitalize`
   uses the JVMs default locale, which may not give you what you
   expect; see https://en.wikipedia.org/wiki/Dotted_and_dotless_I"

  [s]
  (if (< (count s) 2)
    (.toUpperCase s java.util.Locale/ENGLISH)
    (str (.toUpperCase (subs s 0 1) java.util.Locale/ENGLISH)
         (subs s 1))))

(defn reflect-on-method [clazz m]
  (let [n (.getName m)
        return-type (.getReturnType m)
        parameter-types (into [] (.getParameterTypes m))]
    {:method-name n
     :get-property (-> (re-matches #"get(.+)" n) second)
     :set-property (-> (re-matches #"set(.+)" n) second)
     :mutate-property (when (and (= clazz return-type)
                                 (= 1 (count parameter-types)))
                        (capitalize n))
     :return-type return-type 
     :parameter-types parameter-types}))

(def reflect-on-method*
  (memoize reflect-on-method))

(defn reflect-on
  "Inspects via reflection all methods of the DTO `clazz` and
   determines the __properties__ by looking at __getter__ and
   __setter__ methods.

   Throws if any of the properties are __inconsistent__ (e.g. type
   mismatch between getter and setter, wrong numer ob arguments, wrong
   return type)."

  [clazz]
  (reduce
   (fn [res {:keys [method-name get-property set-property mutate-property return-type parameter-types] :as m}]
     ;; Object.getClass() is not a DTO property-getter! TBD: do we
     ;; need this?
     (if-let [property-name (let [x (or get-property set-property mutate-property)]
                              (when-not (= "Class" x) x))]
       ;; method is a getter or setter
       (assoc res property-name 
              (if-let [{:keys [property-type property-getter property-setter property-mutator] :as p} (res property-name)]
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
                            
                            :else method-name))

                  :property-mutator
                  (if-not mutate-property property-mutator
                          (cond
                            property-mutator
                            (throw (ex-info "Duplicate mutator" {:res res :m m}))
                            
                            (not= property-type (first parameter-types))
                            (throw (ex-info "Mutator's type mismatch" {:res res :m m}))
                            
                            :else method-name)))

                ;; seeing the property the first time
                {:property-name property-name
                 :property-type (if (or set-property mutate-property)
                                  (first parameter-types)
                                  return-type)
                 :property-mutator (when mutate-property 
                                     method-name)
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
       res))
   (sorted-map) ;; Start value is an empty map
   (map (partial reflect-on-method* clazz) (.getMethods clazz))))

(def reflect-on*
  (memoize reflect-on))

(declare handler)

;; https://i-proving.com/2008/02/11/the-pitfalls-of-dynamic-proxy-serialization/
(defn make-serializable-invocation-handler [handler-fn dto-type]
  (proxy [java.lang.reflect.InvocationHandler deeto.ISerializable] []
    (writeReplace []
      (let [v {:value (handler-fn nil nil nil) :dto-type dto-type}]
        (deeto.SerializableInvocationHandler. v)))
    (invoke [the-proxy the-method the-args]
      (handler-fn the-proxy the-method the-args))))

(defn make-proxy
  "Returns a Java dynamic proxy (_Deeto proxy_) for the given
   interface(s). Any invocation on the proxy will be delegated to the
   `handler-fn` which should be a function of `the-method` and
   `the-args` of `java.lang.reflect.InvocationHandler/invoke`.

   Calling the overloaded 1-arity variant will create a `handler-fn`
   which keeps an internal state `atom` and delegates calls and the
   state to `handler` (see below)."
  
  ([proxy-type]
     (let [properties (reflect-on* proxy-type)
           state (atom (into (sorted-map)
                             (map #(-> [% nil]) (keys properties))))]
       (make-proxy [proxy-type java.io.Serializable Cloneable]
                   (partial #'handler proxy-type properties state))))
  ([proxy-type handler-fn]
     (let [pt (if (vector? proxy-type)
                (into-array proxy-type)
                (into-array [proxy-type]))]
       (java.lang.reflect.Proxy/newProxyInstance
        (.getClassLoader (first pt)) 
        pt
        (make-serializable-invocation-handler handler-fn (first pt))))))

(defn handle-equals
  "Returns `true` if `@state` and the first value of `the-args` are
   equal in terms of `java.util.Arrays/deepEquals`."

  [clazz state the-method the-args]
  (let [the-args (into [] the-args)
        other (first the-args)]
    (cond 
      (not= 1 (count the-args))
      (throw (ex-info "Not a valid equals method" {:the-method the-method :the-args the-args}))
      
      (nil? other)
      false

      (not= [clazz java.io.Serializable Cloneable] (->> other .getClass .getInterfaces (into [])))
      false
      
      :else 
      (let [s1 (into-array Object (vals @state))
            s2 (into-array Object (vals (.invoke
                                         (java.lang.reflect.Proxy/getInvocationHandler other)
                                         nil nil nil)))]
        (java.util.Arrays/deepEquals s1 s2)))))

(defn handler [clazz properties state the-proxy the-method the-args]
  ;; Special access to @state via null method
  (if-not the-method @state
          (let [{:keys [method-name get-property set-property mutate-property return-type parameter-types]}
                (reflect-on-method* clazz the-method)]
            (cond
              ;; getter returns deep-copy/clone of property's
              ;; value. References to internal state do not leak to
              ;; the outside (see "clone" below)
              get-property (ser-de-ser (@state get-property))
              
              ;; setter creates deep-copy/clone of argument value
              ;; before making the copy/clone part of the internal
              ;; state. So references in/to the argument value will
              ;; not become part of the internal state and thus do not
              ;; leak to the outside (see "clone" below)
              set-property (swap! state assoc set-property (ser-de-ser (first the-args)))

              ;; build-mutator acts like a setter but returns "this"
              ;; for method chaining. Note that we *could* return a
              ;; new instance BUT WE DO mutate. This is not a "factory
              ;; with an argument"
              mutate-property (do
                                (swap! state assoc mutate-property (ser-de-ser (first the-args)))
                                the-proxy)
              
              ;; TBD: how does this behave if serial form of DTO type
              ;; evolves (insert/remove properties, change property
              ;; type)? Do we consider ALL properties or only the ones
              ;; which are part of the current DTOs contract?
              (= "equals" method-name) (handle-equals clazz state the-method the-args)

              ;; consistent with equals(?) (TBD: serial form of DTOs
              ;; type evolves?)
              (= "hashCode" method-name) (java.util.Arrays/deepHashCode (into-array Object (vals @state)))

              ;; Note: for clone we do **not** need to create a deep
              ;; copy - or any copy really! I.e. we can _re-use_
              ;; @state's value **as-is**. Since we'll never mutate
              ;; internal values (@state map is immutable/persistent
              ;; anyways, but the map's values may be mutable) and we
              ;; never leak @state maps's value references to the
              ;; outside there is no danger that anyone ever changes
              ;; state. So effectivly we hava constant immutable
              ;; values (which you need not clone). So all we do need
              ;; is a new proxy/mutable state container (with the
              ;; *same* value).
              (= "clone" method-name)
              (make-proxy [clazz java.io.Serializable Cloneable]
                          (partial #'handler clazz properties (atom @state)))
              
              (= "toString" method-name) (str {:type clazz :value @state})

              :else (throw (ex-info "Unknown invocation"
                                    {:properties properties
                                     :state state
                                     :the-method the-method
                                     :the-args (into [] the-args)}))))))

(defn -init [v]
  [[] v])
  
(defn -writeReplace [this]
  this)

(defn -readResolve [this]
  (let [{:keys [value dto-type]} (.state this)
        properties (reflect-on* dto-type)
        state (atom value)
        handler-fn (partial #'handler dto-type properties state)]
    (make-serializable-invocation-handler handler-fn dto-type)))
