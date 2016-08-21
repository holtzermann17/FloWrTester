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
  (:require [clojure.string :as str]
            [clojure.test :as test]
            [clojure.set :as set]))

;; one test to rule them all...
(defn boolean? [x]
  (or (true? x) (false? x)))
;; [OR ALTERNATIVELY, USE A LATTICE HERE,
;;  ... if we want different degrees of satisfaction of tests.]

;; IsRegex.java

(defn test-string-is-regex 
  "Examples: \"abc\" is a regex, \"[^abc\" is not."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (try (do (java.util.regex.Pattern/compile x)
           true)
       (catch java.util.regex.PatternSyntaxException e false)))
  
;; StringInList.java
;; The requirement to be boolean means being a bit
;; pedantic about true and false (as opposed to "nil", which is "falsey").

(defn test-string-in-list
  "."
  [x y]
  {:pre [(string? x)
         (vector? y)]
   :post [(boolean? %)]}
  (if (some #(= x %) y)
    true
    false))

;; PositiveInteger.java

(defn test-integer-positive
  "."
  [x]
  {:pre [(integer? x)]
   :post [(boolean? %)]}
  (> x 0))

;; NonNegativeInteger.java

(defn test-integer-nonnegative
  "."
  [x]
  {:pre [(integer? x)]
   :post [(boolean? %)]}
  (>= x 0))

;; IntAsString.java

(defn test-string-represents-integer
  "Examples: \"123\" represents an integer, \"12a\" does not."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (try (do (Integer/parseInt x)
           true)
       (catch NumberFormatException e false)))

;; IntAsStringOrAll.java
;; This example illustrates one test calling another

(defn test-string-integer-or-all
  "Allow a string that represents an integer, or the literal \"all\"."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (or (test-string-represents-integer x)
      (= "all" x)))

;; IntInRange.java

(defn test-integer-in-range
  "."
  [x y z]
  {:pre [(integer? x) (integer? y) (integer? z)]
   :post [(boolean? %)]}
  (and (>= x y)
       (<= x z)))

;; FloatInRange.java
;; For now assuming the range is defined by integers, which is a little weird

(defn test-float-in-range
  "."
  [x y z]
  {:pre [(float? x) (integer? y) (integer? z)]
   :post [(boolean? %)]}
  (and (>= x y)
       (<= x z)))

;; IntLessThan.java

(defn test-integer-less-than
  "."
  [x y]
  {:pre [(integer? x) (integer? y)]
   :post [(boolean? %)]}
  (< x y))

;; IntLeqThan.java

(defn test-integer-less-than-or-equal
  "."
  [x y]
  {:pre [(integer? x) (integer? y)]
   :post [(boolean? %)]}
  (<= x y))

;; IntGreaterThan.java

(defn test-integer-greater-than
  "."
  [x y]
  {:pre [(integer? x) (integer? y)]
   :post [(boolean? %)]}
  (> x y))

;; IntGeqThan.java

(defn test-integer-greater-than-or-equal
  "."
  [x y]
  {:pre [(integer? x) (integer? y)]
   :post [(boolean? %)]}
  (>= x y))

;; IsWord.java
;; We could write a more robust variant of this function that checks
;; e.g. whether the word is in wordnet.  (Note that Wordnet does
;; not contain all words, e.g. it doesn't contain pronouns.)

(defn word?
  "Allow a string that represents a word, defined by not containing spaces."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (if (some #(= \space %) (seq x))
    false
    true))

;; This is an example of a test whose precondition is defined in terms of
;; another test - this shows that we're effectively doing a kind of subtyping.
(defn noun?
  "Allow a word that has a noun sense."
  [x]
  {:pre [(word? x)]
   :post [(boolean? %)]}
  (if (empty? (flowrweb.core/wordnet x :noun))
    false
    true))

(defn verb?
  "Allow a word that has a verb sense."
  [x]
  {:pre [(word? x)]
   :post [(boolean? %)]}
  (if (empty? (flowrweb.core/wordnet x :verb))
    false
    true))

;; ExclamSeparatedWords.java

(defn test-string-exclam-separated-words
  "."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (loop [items (str/split x #"!!")]
    ;; if there are no more items left, then we've successfully made it to the end of the list
    (if (empty? items)
      true
      ;; otherwise, test the next item and proceed accordingly
      (if (word? (first items))
        (recur (rest items))
        false))))

;; ExclamSeparatedInts.java

(defn test-string-exclam-separated-ints
  "."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (loop [items (str/split x #"!!")]
    (if (empty? items)
      true
      (if (test-string-represents-integer (first items))
        (recur (rest items))
        false))))

;; ExclamSeparatedIntsOrAll.java

(defn test-string-exclam-separated-ints-or-all
  "x is a string that is either a !! list of integers or the singular symbol \"all\"."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (if (= x "all")
    true
    (loop [items (str/split x #"!!")]
      (if (empty? items)
        true
        (if (test-string-represents-integer (first items))
          (recur (rest items))
          false)))))

;; SemicolonSeparatedWords.java

(defn test-string-semicolon-separated-words
  "."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (loop [items (str/split x #";")]
    (if (empty? items)
      true
      (if (word? (first items))
        (recur (rest items))
        false))))

;; A space-separated list of words is a phrase.
;; (Will this ever fail?)
(defn test-string-is-phrase
  "."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (loop [items (str/split x #" ")]
    (if (empty? items)
      true
      (if (word? (first items))
        (recur (rest items))
        false))))

(defn test-string-semicolon-separated-phrases
  "."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (loop [items (str/split x #";")]
    (if (empty? items)
      true
      (if (test-string-is-phrase (first items))
        (recur (rest items))
        false))))

;; ExclamSeparatedItemsFromList.java
;; here we might further want to guarantee that y is a list of strings...

(defn test-string-exclam-separated-items-from-list
  "."
  [x y]
  {:pre [(string? x)
         (vector? y)]
   :post [(boolean? %)]}
  (loop [items (str/split x #"!!")]
    (if (empty? items)
      true
      (if (test-string-in-list (first items) y)
        (recur (rest items))
        false))))

;; IntMinimizesTuplesLengths.java

(defn test-integer-minimises-tuples-lengths
  "."
  [x y]
  {:pre [(integer? x)
         (vector? y)
         (every? vector? y)]
   :post [(boolean? %)]}
  ;; note that if we didn't put x in here, we could use this function
  ;; to test whether the vector y had monotonically incleaning length
  (apply < x (map count y)))

;; UnderscoreSeparatedWords.java

(defn test-string-semicolon-separated-words
  "."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (loop [items (str/split x #"_")]
    (if (empty? items)
      true
      (if (word? (first items))
        (recur (rest items))
        false))))

;; FloatAsString.java

(defn test-string-represents-float
  "Examples: \"1.23\" represents an float, \"12a.23\" does not."
  [x]
  {:pre [(string? x)]
   :post [(boolean? %)]}
  (try (do (Float/parseFloat x)
           true)
       (catch NumberFormatException e false)))

;; EachOne.java

(defn test-each-one
  "x is a test, y is a vector, and every element of y satisfies the test x"
  [x y]
  {:pre [(test/function? x)
         (vector? y)]
   :post [(boolean? %)]}
  (every? x y))

;; EachOneInPlaces.java

(defn test-each-one-in-places
  "x is a test, y is a vector, z denotes the places in y that must pass the test x."
  [x y z]
  {:pre [(test/function? x)
         (vector? y)
         (test-string-exclam-separated-ints-or-all z)]
   :post [(boolean? %)]}
  (if (= z "all")
    (test-each-one x y)
    ;; convert the denoted integers to genuine integers
    (let [indices (map #(Integer/parseInt %) (str/split z #"!!"))]
      ;; retain the items in matching places, and apply the test to them
      ;; see: http://stackoverflow.com/questions/7744656/how-do-i-filter-elements-from-a-sequence-based-on-indexes
      ;; http://www.spacjer.com/blog/2015/11/24/lesser-known-clojure-keep-and-keep-indexed-functions/
      (test-each-one x (into [] (keep-indexed #(when ((set indices) %1) %2) y))))))

;; SelectionFrom.java

;; e.g. could use a value like percentage of overlap here
(defn test-selection-from
  "x is a vector, y is a vector that should be entirely comprised of elements selected from x."
  [x y]
  {:pre [(vector? x)
         (vector? y)]
   :post [(boolean? %)]}
  (set/subset? (set y) (set x)))

