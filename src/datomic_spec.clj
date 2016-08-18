(ns datomic-spec
  (:require [clojure.spec :as s]
            [clojure.string :refer [ends-with? starts-with?]]
            [clojure.test.check.generators :as gen]))

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
                                        (starts-with? (name %) "?")
                                        (= '_ %))))]
    (s/with-gen
      pred
      #(gen/such-that pred gen/symbol))))

;; variable = symbol starting with "?"
(s/def ::variable
  (prefixed-symbol-spec "?"))

;; not-clause = [ src-var? 'not' clause+ ]
(s/def ::not-clause
  (s/cat :condition (s/spec (s/cat :src-var (s/? ::src-var)
                                   :not #{'not}
                                   :clauses (s/+ ::clause)))))

;; not-join-clause = [ src-var? 'not-join' [variable+] clause+ ]
(s/def ::not-join-clause
  (s/cat :condition (s/spec (s/cat :src-var (s/? ::src-var)
                                   :not-join #{'not-join}
                                   :variables (s/spec (s/+ ::variable))
                                   :clauses (s/+ ::clause)))))

;; and-clause = [ 'and' clause+ ]
(s/def ::and-clause
  (s/cat :condition (s/spec (s/cat :and #{'and}
                                   :clauses (s/+ ::clause)))))

;; or-clause = [ src-var? 'or' (clause | and-clause)+]
(s/def ::or-clause
  (s/cat :condition (s/spec (s/cat :src-var (s/? ::src-var)
                                   :or #{'or}
                                   :clauses (s/+ (s/alt :and-clause ::and-clause
                                                        :clause ::clause))))))

;; FIXME this is definitely wrong
;; rule-vars = [variable+ | ([variable+] variable*)]
(s/def ::rule-vars
  (s/or :variables (s/+ ::variable)
        :required-variables (s/cat :req (s/spec (s/+ ::variable))
                                   :opt (s/* ::variable))))

;; or-join-clause = [ src-var? 'or-join' rule-vars (clause | and-clause)+ ]
;; FIXME this takes rule-vars?
(s/def ::or-join-clause
  (s/cat :condition (s/spec
                      (s/cat :src-var (s/? ::src-var)
                             :or-join #{'or-join}
                             :variables (s/spec (s/+ ::variable))
                             :clauses (s/+ (s/alt :and-clause ::and-clause
                                                  :clause ::clause))))))

;; data-pattern = [ src-var? (variable | constant | '_')+ ]
(s/def ::data-pattern
  (s/cat :src-var (s/? ::src-var)
         :pattern (s/+ (s/alt :variable ::variable
                              :constant ::constant
                              :blank #{'_}))))

;; fn-arg = (variable | constant | src-var)
(s/def ::fn-arg
  (s/alt :variable ::variable
         :constant ::constant
         :src-var ::src-var))

;; pred-expr = [ [pred fn-arg+] ]
(s/def ::pred-expr
  (s/cat :expr (s/spec
                 (s/cat :fn-call (s/spec (s/cat :fn ::plain-symbol
                                                :fn-args (s/+ ::fn-arg)))))))

;; bind-scalar = variable
(s/def ::bind-scalar ::variable)

;; bind-tuple = [ (variable | '_')+]
(s/def ::bind-tuple
  (s/cat :tuple (s/spec (s/+ (s/alt :variable ::variable
                                    :blank #{'_})))))

;; bind-coll = [variable '...']
(s/def ::bind-coll
  (s/cat :coll (s/spec (s/cat :variable ::variable
                              :ellipses #{'...}))))

;; bind-rel = [ [(variable | '_')+]]
(s/def ::bind-rel
  (s/cat :find-rel (s/spec
                     (s/cat :variables (s/spec (s/+ (s/alt :variable ::variable
                                                           :blank #{'_})))))))

;; binding = (bind-scalar | bind-tuple | bind-coll | bind-rel)
(s/def ::binding
  (s/alt :bind-scalar ::bind-scalar
         :bind-tuple ::bind-tuple
         :bind-coll ::bind-coll
         :bind-rel ::bind-rel))

;; fn-expr = [ [fn fn-arg+] binding]
(s/def ::fn-expr
  (s/cat :expr (s/spec
                 (s/cat :fn-call (s/spec (s/cat :fn ::plain-symbol
                                                :fn-args (s/+ ::fn-arg)))
                        :binding ::binding))))

;; rule-expr = [ src-var? rule-name (variable | constant | '_')+]
(s/def ::rule-expr
  (s/cat :src-var (s/? ::src-var)
         :rule-name ::plain-symbol
         :rule-args (s/+ (s/alt :variable ::variable
                                :constant ::constant
                                :blank #{'_}))))

;; expression-clause = (data-pattern | pred-expr | fn-expr | rule-expr)
(s/def ::expression-clause
  (s/alt :pred-expr ::pred-expr
         :fn-expr ::fn-expr
         :rule-exp (s/spec ::rule-expr)
         :data-pattern (s/spec ::data-pattern)))

;; clause = (not-clause | not-join-clause | or-clause | or-join-clause |
;;           expression-clause)
(s/def ::clause
  (s/alt :not-clause ::not-clause
         :not-join-clause ::not-join-clause
         :or-clause ::or-clause
         :or-join-clause ::or-join-clause
         :expression-clause ::expression-clause))

;; src-var = symbol starting with "$"
(s/def ::src-var
  (prefixed-symbol-spec "$"))

;; inputs = ':in' (src-var | variable | pattern-var | rules-var)+
(s/def ::input
  (s/alt :src-var ::src-var
         :variable ::variable
         :pattern-var ::plain-symbol
         :rules-var #{'%}
         :binding ::binding))

;; aggregate = [aggregate-fn-name fn-arg+]
(s/def ::aggregate
  (s/cat :fn-call (s/spec (s/cat :fn-name ::plain-symbol
                                 :fn-args (s/+ ::fn-arg)))))

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
  (s/cat :coll (s/spec (s/cat :elem ::find-elem
                              :ellipses #{'...}))))

;; find-tuple = [find-elem+]
(s/def ::find-tuple
  (s/cat :tuple (s/spec (s/+ ::find-elem))))

;; find-scalar = find-elem '.'
(s/def ::find-scalar
  (s/cat :elem ::find-elem
         :period #{'.}))

;; find-spec = ':find' (find-rel | find-coll | find-tuple | find-scalar)
(s/def ::find-spec
  (s/alt :find-rel ::find-rel
         :find-coll ::find-coll
         :find-tuple ::find-tuple
         :find-scalar ::find-scalar))

;; where-clauses = ':where' clause+
(s/def ::where
  (s/cat :clauses (s/+ ::clause)))

;; with-clause = ':with' variable+
(s/def ::with
  (s/cat :variables (s/+ ::variable)))

(s/def ::in
  (s/cat :inputs (s/+ ::input)))

(s/def ::find
  (s/cat :spec ::find-spec))

(defmulti query-form (fn [query] (if (map? query) :map :list)))

(defmethod query-form :map [_]
  (s/keys :req-un [::find] :opt-un [::with ::in ::where]))

(defmethod query-form :list [_]
  (s/cat :find (s/cat :find-kw #{:find} :spec ::find-spec)
         :with (s/? (s/cat :with-kw #{:with} :variables (s/+ ::variable)))
         :in (s/? (s/cat :in-kw #{:in} :inputs (s/+ ::input)))
         :where (s/? (s/cat :where-kw #{:where} :clauses (s/+ ::clause)))))

(s/def ::query
  (s/multi-spec query-form (fn [g _] g)))

(s/def ::attr-name keyword?)

;; recursion-limit = positive-number | '...'
(s/def ::recursion-limit
  (s/alt :count pos-int?
         :ellipses #{"..." '...}))

;; default-expr = [("default" | 'default') attr-name any-value]
(s/def ::default-expr
  (s/cat :expr (s/spec (s/cat :default #{"default" 'default}
                              :attr-name ::attr-name
                              :any-value (let [pred #(not (symbol? %))]
                                           (s/with-gen
                                             pred
                                             #(gen/such-that
                                               pred
                                               gen/simple-type-printable)))))))

;; limit-expr = [("limit" | 'limit') attr-name (positive-number | nil)]
(s/def ::limit-expr
  (s/cat :expr(s/spec
                (s/cat :limit #{"limit" 'limit}
                       :attr-name ::attr-name
                       :count (s/nilable pos-int?)))))

;; attr-expr = limit-expr | default-expr
(s/def ::attr-expr
  (s/alt :limit-expr ::limit-expr
         :default-expr ::default-expr))

;; map-spec = { ((attr-name | limit-expr) (pattern | recursion-limit))+ }
(s/def ::map-spec
  (s/map-of (s/or :attr-name ::attr-name
                  :limit-expr ::limit-expr)
            (s/or :pattern ::pull-pattern
                  :recursion-limit ::recursion-limit)))

;; wildcard = "*" or '*'
(s/def ::wildcard
  (s/cat :asterisk #{"*" '*}))

;; attr-spec = attr-name | wildcard | map-spec | attr-expr
(s/def ::attr-spec
  (s/alt :attr-name ::attr-name
         :wildcard ::wildcard
         :map-spec ::map-spec
         :attr-expr ::attr-expr))

;; pattern = [attr-spec+]
(s/def ::pull-pattern
  (s/cat :pattern (s/+ ::attr-spec)))