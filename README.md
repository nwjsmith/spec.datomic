# spec.datomic

[![CircleCI](https://circleci.com/gh/nwjsmith/datomic-spec/tree/master.svg?style=svg)](https://circleci.com/gh/nwjsmith/datomic-spec/tree/master)

`clojure.spec` specs for Datomic's query data. **Note** this is under active development, so you will get very little use out of it right now.

## Usage

```clojure
(require 'com.theinternate.spec.datomic)
(require 'clojure.spec)
(clojure.spec/valid? ::datomic-spec/query '{:find [?e]
                                            :in [$ ?fname ?lname]
                                            :where [[?e :user/firstName ?fname]
                                                    [?e :user/lastName ?lname]]})
;; => true
(clojure.spec/valid? ::datomic-spec/query '[:find [?name ...]
                                            :in $ ?artist
                                            :where [?release :release/name ?name]
                                                   [?release :release/artists ?artist]])
;; => true

(clojure.spec/valid? ::datomic-spec/query '[:find ?e
                                            :inputs $ ?artist
                                            :where [?e :release/artists ?artist]])
;; => false
```

## License

Copyright © 2016 Nate Smith

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
