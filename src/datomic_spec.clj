(ns datomic-spec
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]))

;; clause = (not-clause | not-join-clause | or-clause | or-join-clause |
;;           expression-clause)
(s/def ::clause
  (s/alt :not ::s/any
         :not-join ::s/any
         :or ::s/any
         :or-join ::s/any
         :expression ::s/any))

;; where-clauses = ':where' clause+
(s/def ::where
  (s/cat :clauses (s/+ ::clause)))

(s/def ::in-var
  (s/alt :src symbol?
         :var symbol?
         :pattern symbol?
         :rules symbol?))

;; inputs = ':in' (src-var | variable | pattern-var | rules-var)+
(s/def ::in
  (s/cat :vars (s/+ ::in-var)))

(s/def ::with-var symbol?)

;; with-clause = ':with' variable+
(s/def ::with
  (s/cat :vars (s/+ ::with-var)))

(s/def ::var
  (s/with-gen
    (s/and symbol? #(str/starts-with? (name %) "?"))
    #(gen/fmap
       (fn [n] (symbol (str n "?")))
       (gen/not-empty gen/string-alphanumeric))))

;; TODO: pull-expr
;; find-elem = (variable | pull-expr | aggregate)
(s/def ::find-elem
  (s/or :var ::var
        :aggregate ::s/any))

;; find-rel = find-elem+
(s/def ::find-rel
  (s/+ ::find-elem))

(s/def ::find-spec
  (s/or :rel ::find-rel
        :coll ::s/any
        :tuple ::s/any
        :scalar ::s/any))

;; find-spec = ':find' (find-rel | find-coll | find-tuple | find-scalar)
(s/def ::find
  (s/cat :spec ::find-spec))

;; Stolen from core.incubator
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

(def ^:private query-spec
  (s/or :map (s/keys :req-un [::find] :opt-un [::with ::in ::where])
        :list (s/cat :find (s/cat :find-kw #{:find} :spec ::find-spec)
                     :with (s/? (s/cat :with-kw #{:with}
                                       :vars (s/+ ::with-var)))
                     :in (s/? (s/cat :in-kw #{:in} :vars (s/+ ::in-var)))
                     :where (s/? (s/cat :where-kw #{:where}
                                        :clauses (s/+ ::clause))))))

(defn- query-conformer
  [x]
  (let [conformed (s/conform query-spec x)]
    (if (= conformed ::s/invalid)
      ::s/invalid
      (let [[_ data] conformed]
        (-> data
            (dissoc-in [:find :find-kw])
            (dissoc-in [:with :with-kw])
            (dissoc-in [:in :in-kw])
            (dissoc-in [:where :where-kw]))))))

;; query = [find-spec with-clause? inputs? where-clauses?]
(s/def ::query
  (s/with-gen
    (s/conformer query-conformer)
    #(s/gen query-spec)))