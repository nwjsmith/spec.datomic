(ns datomic-spec
  (:require [clojure.spec :as s]
            [clojure.string :refer [ends-with? starts-with?]]
            [clojure.test.check.generators :as gen]))

;; From core.incubator
(defn- dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn prefixed-symbol-spec
  "Takes a prefix and returns a spec for a symbol with that prefix."
  [prefix]
  (s/with-gen
    (s/and symbol? #(starts-with? (name %) prefix))
    #(gen/fmap
      (fn [n] (symbol (str prefix n)))
      (gen/not-empty gen/string-alphanumeric))))

;; =============================================================================
;; Specs

;; constant = any non-variable data literal
(s/def ::constant
  (let [pred #(not (symbol? %))]
    (s/with-gen
      pred
      #(gen/such-that pred gen/simple-type-printable))))

;; plain-symbol = symbol that does not begin with "$" or "?"
(s/def ::plain-symbol
  (let [pred #(and (symbol? %) (not (or (starts-with? (name %) "$")
                                        (starts-with? (name %) "?"))))]
    (s/with-gen
      pred
      #(gen/such-that pred gen/symbol))))

;; variable = symbol starting with "?"
(s/def ::variable
  (prefixed-symbol-spec "?"))

;; not-clause = [ src-var? 'not' clause+ ]
(s/def ::not-clause
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :not #{'not}
                 :clauses (s/+ ::clause))))

;; not-join-clause = [ src-var? 'not-join' [variable+] clause+ ]
(s/def ::not-join-clause
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :not-join #{'not-join}
                 :variables (s/+ ::variable)
                 :clauses (s/+ ::clause))))

;; and-clause = [ 'and' clause+ ]
(s/def ::and-clause
  (s/spec (s/cat :and #{'and}
                 :clauses (s/+ ::clause))))

;; or-clause = [ src-var? 'or' (clause | and-clause)+]
(s/def ::or-clause
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :or #{'or}
                 :clauses (s/+ (s/alt :clause ::clause
                                      :and-clause ::and-clause)))))

;; FIXME this is definitely wrong
;; rule-vars = [variable+ | ([variable+] variable*)]
(s/def ::rule-vars
  (s/or :variables (s/+ ::variable)
        :required-variables (s/cat :req (s/spec (s/+ ::variable))
                                   :opt (s/* ::variable))))

;; or-join-clause = [ src-var? 'or-join' rule-vars (clause | and-clause)+ ]
(s/def ::or-join-clause
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :or-join #{'or-join}
                 :rule-vars ::rule-vars
                 :clauses (s/+ (s/alt :clause ::clause
                                      :and-clause ::and-clause)))))

;; data-pattern = [ src-var? (variable | constant | '_')+ ]
(s/def ::data-pattern
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :pattern (s/+ (s/alt :variable ::variable
                                      :constant ::constant
                                      :blank #{'_})))))

;; fn-arg = (variable | constant | src-var)
(s/def ::fn-arg
  (s/alt :variable ::variable
         :constant ::constant
         :src-var ::src-var))

;; pred = FIXME this probably isn't a narrow enough
(s/def ::pred
  (s/with-gen
    (s/and symbol? #(ends-with? (name %) "?"))
    #(gen/fmap
      (fn [s] (symbol (str s "?")))
      (gen/not-empty gen/string-alphanumeric))))

;; pred-expr = [ [pred fn-arg+] ]
(s/def ::pred-expr
  (s/spec
    (s/spec (s/cat :pred ::pred
                   :fn-args (s/+ ::fn-arg)))))

;; bind-scalar = variable
(s/def ::bind-scalar ::variable)

;; bind-tuple = [ (variable | '_')+]
(s/def ::bind-tuple
  (s/spec (s/+ (s/alt :variable ::variable
                      :blank #{'_}))))

;; bind-coll = [variable '...']
(s/def ::bind-coll
  (s/spec (s/cat :variable ::variable
                 :ellipses #{'...})))

;; bind-rel = [ [(variable | '_')+]]
(s/def ::bind-rel
  (s/spec
    (s/spec (s/+ (s/alt :variable ::variable
                        :blank #{'_})))))

;; binding = (bind-scalar | bind-tuple | bind-coll | bind-rel)
(s/def ::binding
  (s/alt :bind-scalar ::bind-scalar
         :bind-tuple ::bind-tuple
         :bind-coll ::bind-coll
         :bind-rel ::bind-rel))

;; fn-expr = [ [fn fn-arg+] binding]
(s/def ::fn-expr
  (s/spec
    (s/spec (s/cat :fn ::plain-symbol
                   :fn-args (s/+ ::fn-arg)
                   :binding ::binding))))

;; rule-expr = [ src-var? rule-name (variable | constant | '_')+]
(s/def ::rule-expr
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :rule-name ::plain-symbol
                 :rule-args (s/+ (s/alt :variable ::variable
                                        :constant ::constant
                                        :blank #{'_})))))

;; expression-clause = (data-pattern | pred-expr | fn-expr | rule-expr)
(s/def ::expression-clause
  (s/alt :data-pattern ::data-pattern
         :pred-expr ::pred-expr
         :fn-expr ::fn-expr
         :rule-exp ::rule-expr))

;; clause = (not-clause | not-join-clause | or-clause | or-join-clause |
;;           expression-clause)
(s/def ::clause
  (s/alt :not-clause ::not-clause
         :not-join ::not-join-clause
         :or-clause ::or-clause
         :or-join-clause ::or-join-clause
         :expression-clause ::expression-clause))

;; where-clauses = ':where' clause+
(s/def ::where
  (s/cat :clauses (s/+ ::clause)))

;; src-var = symbol starting with "$"
(s/def ::src-var
  (prefixed-symbol-spec "$"))

;; inputs = ':in' (src-var | variable | pattern-var | rules-var)+
(s/def ::input
  (s/alt :src-var ::src-var
         :variable ::variable
         :pattern-var ::plain-symbol
         :rules-var #{'%}))

;; aggregate = [aggregate-fn-name fn-arg+]
(s/def ::aggregate
  (s/spec (s/cat :fn-name symbol?
                 :fn-args (s/+ ::fn-arg))))

;; TODO: pull-expr
;; find-elem = (variable | pull-expr | aggregate)
(s/def ::find-elem
  (s/alt :variable ::variable
         :aggregate ::aggregate))

;; find-rel = find-elem+
(s/def ::find-rel
  (s/+ ::find-elem))

;; find-coll = [find-elem '...']
(s/def ::find-coll
  (s/spec (s/cat :elem ::find-elem
                 :ellipses #{'...})))

;; find-tuple = [find-elem+]
(s/def ::find-tuple
  (s/spec (s/+ ::find-elem)))

;; find-scalar = find-elem '.'
(s/def ::find-scalar
  (s/cat :elem ::find-elem
         :period #{'.}))

;; find-spec = ':find' (find-rel | find-coll | find-tuple | find-scalar)
(s/def ::find-spec
  (s/alt :rel ::find-rel
         :coll ::find-coll
         :tuple ::find-tuple
         :scalar ::find-scalar))

;; with-clause = ':with' variable+
(s/def ::with
  (s/cat :variables (s/+ ::variable)))

(s/def ::in
  (s/cat :inputs (s/+ ::input)))

(s/def ::find
  (s/cat :spec ::find-spec))

(s/def ::query-map
  (s/keys :req-un [::find] :opt-un [::with ::in ::where]))

(s/def ::list-find (s/cat :find-kw #{:find} :spec ::find-spec))

(s/def ::list-with (s/? (s/cat :with-kw #{:with} :variables (s/+ ::variable))))

(s/def ::list-in (s/? (s/cat :in-kw #{:in} :inputs (s/+ ::input))))

(s/def ::list-where (s/? (s/cat :where-kw #{:where} :clauses (s/+ ::clause))))

(s/def ::query-list
  (s/cat :find ::list-find
         :with ::list-with
         :in ::list-in
         :where ::list-where))

(def ^:private list-form-kw-paths
  [[:find :find-kw]
   [:with :with-kw]
   [:in :in-kw]
   [:where :where-kw]])

(def ^:private query-spec
  (s/or :map ::query-map
        :list ::query-list))

(defn- conform-query
  "Takes a query in list or map form and conforms it, stripping query
  form-specific data."
  [x]
  (let [conformed (s/conform query-spec x)]
    (if (= conformed ::s/invalid)
      ::s/invalid
      (let [[_query-form data] conformed]
        (reduce (fn [m path] (dissoc-in m path))
                data
                list-form-kw-paths)))))

;; query = [find-spec with-clause? inputs? where-clauses?]
(s/def ::query
  (reify
    clojure.lang.IFn
    (invoke [this x] (s/valid? this x))
    clojure.spec/Spec
    (conform* [_ m]
      (let [conformed (s/conform query-spec m)]
        (if (= conformed ::s/invalid)
          ::s/invalid
          (let [[_query-form data] conformed]
            (reduce (fn [m path] (dissoc-in m path))
                    data
                    list-form-kw-paths)))))
    (explain* [_ path via in x] (s/explain* query-spec path via in x))
    (gen* [_ overrides path rmap] (s/gen* query-spec overrides path rmap))
    (with-gen* [_ gfn] (s/with-gen* query-spec gfn))
    (describe* [_] (s/describe* query-spec))))
