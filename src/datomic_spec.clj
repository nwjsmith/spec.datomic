(ns datomic-spec
  (:require [clojure.spec :as s]))

;; clause = (not-clause | not-join-clause | or-clause | or-join-clause |
;;           expression-clause)
(s/def ::clause
  (s/alt :not-clause ::s/any
         :not-join-clause ::s/any
         :or-clause ::s/any
         :or-join-clause ::s/any
         :expression-clause ::s/any))

;; where-clauses = ':where' clause+
(s/def ::where
  (s/+ ::clause))

;; inputs = ':in' (src-var | variable | pattern-var | rules-var)+
(s/def ::in
  (s/+ (s/alt :src-var symbol?
              :var symbol?
              :pattern-var symbol?
              :rules-var symbol?)))

;; with-clause = ':with' variable+
(s/def ::with
  (s/+ symbol?))

;; find-spec = ':find' (find-rel | find-coll | find-tuple | find-scalar)
(s/def ::find
   (s/or :rel ::s/any
         :coll ::s/any
         :tuple ::s/any
         :scalar ::s/any))

;; query = [find-spec with-clause? inputs? where-clauses?]
(s/def ::query
  (s/or :map (s/keys :req-un [::find] :opt-un [::with ::in ::where])
        :list (s/cat :find (s/cat :find-kw #{:find} :spec ::find)
                     :with (s/? (s/cat :with-kw #{:with} :vars ::with))
                     :in (s/? (s/cat :in-kw #{:in} :variables ::in))
                     :where (s/? (s/cat :where-kw #{:where}
                                        :clauses ::where)))))