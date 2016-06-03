(ns datomic-spec
  (:require [clojure.spec :as s]))

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

(s/def ::with-var
  (s/+ symbol?))

;; with-clause = ':with' variable+
(s/def ::with
  (s/cat :vars (s/+ ::with-var)))

(s/def ::find-spec
  (s/or :rel ::s/any
        :coll ::s/any
        :tuple ::s/any
        :scalar ::s/any))

;; find-spec = ':find' (find-rel | find-coll | find-tuple | find-scalar)
(s/def ::find
  (s/cat :spec ::find-spec))

;; query = [find-spec with-clause? inputs? where-clauses?]
(s/def ::query
  (s/or :map (s/keys :req-un [::find] :opt-un [::with ::in ::where])
        :list (s/cat :find (s/cat :find-kw #{:find} :spec ::find-spec)
                     :with (s/? (s/cat :with-kw #{:with}
                                       :vars (s/+ ::with-var)))
                     :in (s/? (s/cat :in-kw #{:in} :vars (s/+ ::in-var)))
                     :where (s/? (s/cat :where-kw #{:where}
                                        :clauses (s/+ ::clause))))))