;; (-> #'builder meta :arglists first meta)

;; To turn off pre- and post- checks
; (set! *assert* false)

;;; A namespace to hold functions with pre and post conditions that we
;;; could wire together in automatically-generated programs.

;; I've started with some tests (from the FloWr documentation, see
;; /home/joe/FloWrTester/ccg/flow/tests) many of which I had written
;; in Java; I'll rewrite them in Clojure now (perhaps using a little
;; bit of interop in some places).

;; This initial set of tests can be extended easily enough with more.

(ns fake.flowrs-tests
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.string :as str]
            [clojure.test :as test]
            [clojure.set :as set]))

;; one test to rule them all...
(s/def ::boolean?
  (s/or :true true? :false false?))

;; [OR ALTERNATIVELY, USE A LATTICE HERE,
;;  ... if we want different degrees of satisfaction of tests.]

;;  Similar to FloWr, we have inputs and outputs of different (sub)types.

;; IsRegex.java

;;  Examples: \"abc\" is a regex, \"[^abc\" is not.
;; e.g. a regular expression...
(s/def ::regex?
  (s/and string?
         #(try (do (java.util.regex.Pattern/compile %)
                   true)
               (catch java.util.regex.PatternSyntaxException
                   e false))))

;; StringInList.java
;; The requirement to be boolean means being a bit
;; pedantic about true and false (as opposed to "nil", which is "falsey").

;; Examples:
;; (s/valid? ::string-in-list? ["abc" ["abc" "def"]]) is TRUE
;; (s/valid? ::string-in-list? ["abc" ["qed" "pdq" "rfd"]]) is FALSE
;;
;; Note: this (gen/generate (s/gen ::string-in-list?)) fails with:
;; ExceptionInfo Couldn't satisfy such-that predicate after 100 tries.
;;
;; Which suggests the need for a custom generator in this case
;;
;; Note, there's a useful video about custom generators here:
;; http://blog.cognitect.com/blog/2016/8/10/clojure-spec-screencast-customizing-generators
;; or a string that's a member of a given list...
(s/def ::string-in-list?
  (s/and (s/cat :needle string? :haystack vector?)
         #(some (fn [item] (= (:needle %) item))
                (:haystack %))))

;; Also note style above: (some (fn [item] (= "abc" item)) ["qed"])
;; returns nil, but the test nicely handles that and returns false
;; in the corresponding case

;; PositiveInteger.java

(s/def ::positive-integer?
  (s/and integer? #(> % 0)))

;; NonNegativeInteger.java

(s/def ::nonnegative-integer?
  (s/and integer? #(>= % 0)))

;; IntAsString.java

;; Note: this (gen/generate (s/gen ::string-represents-integer?))
;; only works intermittently, generating short integer-strings like "3"

(s/def ::string-represents-integer?
  (s/and string?
         #(try (do (Integer/parseInt %)
                   true)
               (catch NumberFormatException e false))))

;; IntAsStringOrAll.java
;; This example illustrates one test calling another

(s/def ::string-integer-or-all?
  (s/or :int ::string-represents-integer?
        :all #(= "all" %)))

;; IntInRange.java

(s/def ::integer-in-range?
  (s/and (s/cat :target integer? :bottom integer? :top integer?)
         #(and (>= (:target %) (:bottom %))
               (<= (:target %) (:top %)))))

;; FloatInRange.java
;; For now assuming the range is defined by integers, which is a little weird

(s/def ::float-in-range?
  (s/and (s/cat :target float? :bottom integer? :top integer?)
         #(and (>= (:target %) (:bottom %))
               (<= (:target %) (:top %)))))

;; IntLessThan.java

(s/def ::integer-less-than?
  (s/and (s/cat :target integer? :top integer?)
         #(< (:target %) (:top %))))

;; IntLeqThan.java

(s/def ::integer-less-than-or-equal?
  (s/and (s/cat :target integer? :top integer?)
         #(<= (:target %) (:top %))))

;; IntGreaterThan.java

(s/def ::integer-greater-than?
  (s/and (s/cat :target integer? :bottom integer?)
         #(> (:target %) (:bottom %))))

;; IntGeqThan.java

(s/def ::integer-greater-than-or-equal?
  (s/and (s/cat :target integer? :bottom integer?)
         #(>= (:target %) (:bottom %))))

;; IsWord.java
;; We could write a more robust variant of this function that checks
;; e.g. whether the word is in wordnet.  (Note that Wordnet does
;; not contain all words, e.g. it doesn't contain pronouns.)

(s/def ::word?
  (s/and string?
         #(> (count (seq %)) 0)
         #(not (some (fn [char] (= \space char))
                     (seq %)))))

;; For now we will insist that a phrase contains no double spaces;
;; (Do we want anything else?)
(s/def ::phrase?
  (s/and string?
         #(every? (fn [term] (s/valid? ::word? term))
                  (str/split % #" "))))

;; another example where we might want a custom generator
(s/def ::semicolon-separated-phrases?
  (s/and string?
         #(every? (fn [term] (s/valid? ::phrase? term))
                  (str/split % #"; ?"))))

;; This is an example of a test whose precondition is defined in terms
;; of another test, which shows how we're effectively doing a kind of
;; subtyping.
;;
;; This example is also somewhat "knowledge-based", in that it refers
;; to an imported function.
;; e.g. "dog" "cat" etc.
(s/def ::noun?
  (s/and ::word?
         #(not (empty? (flowrweb.core/wordnet % :noun)))))

;; e.g. "run" "help" etc.
(s/def ::verb?
  (s/and ::word?
         #(not (empty? (flowrweb.core/wordnet % :verb)))))

;; ExclamSeparatedWords.java

;; unless we have a more restrictive definition of words,
;; then this isn't a very meaningful predicate
; (s/def ::!!-separated-words? ::word?)

;; Let's try something more in the spirit of this module,
;; and worry about modeling the precise details of FloWr
;; data later

(s/def ::words?
  (s/* ::word?))

;; ExclamSeparatedInts.java

(s/def ::strints?
  (s/* ::string-represents-integer?))

;; ExclamSeparatedIntsOrAll.java

(s/def ::strints-or-all?
  (s/or :all #(= "all" %)
        :strints ::strints?))

;; IntMinimizesTuplesLengths.java

;; Let's build this one up in two steps

(s/def ::vector-of-vectors?
  (s/* vector?))

(s/def ::integer-minimises-tuples-lengths?
  (s/and (s/cat :int integer? :vec ::vector-of-vectors?)
         #(apply < (:int %) (map count (:vec %)))))

;; FloatAsString.java

(s/def ::string-represents-float?
  (s/and string?
         #(try (do (Float/parseFloat %)
                   true)
               (catch NumberFormatException e false))))

;; SelectionFrom.java

;; e.g. we could potentially use a value like percentage of overlap
;; if we wanted to deal with non-binary inclusion

(s/def ::selection-from?
  (s/and (s/cat :sub vector? :super vector?)
         #(set/subset? (set (:sub %)) (set (:super %)))))

;;; Some things I'll skip in this edition

;; UnderscoreSeparatedWords.java
;; skip this for now

;; EachOne.java
;; I'm not totally convinced we'll ever need this;
;; if we do, can't we just use plain old `every?`
;; ...or go about it some other way?
;; Check out the earlier implementation if needed

;; EachOneInPlaces.java
;; Again not totally convinced, but we have the earlier implementation
;; if it ever proves useful

;; SemicolonSeparatedWords.java

;; skip this, we can just use ::words?

;; A space-separated list of words is a phrase.
;; (Will this ever fail?)
;; Again, let's skip it for now

;; ExclamSeparatedItemsFromList.java
;; here we might further want to guarantee that y is a list of strings...
;; skip this for now

