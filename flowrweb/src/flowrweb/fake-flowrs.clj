(ns fake.flowrs
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest]
            [clojure.string :as str]))

;;; Simple example

;; This function illustrates that there's a lot of things that
;; can be asserted even about a simple function.  Do we need
;; to assert all of this?  It's only relevant as long as it's
;; interpretable.  I guess it is, if we write it in the spec
;; way.  And when we do it like this, we can generate arbitrary
;; input, see below.

(s/fdef pair-builder
        :args (s/cat :left integer? :right integer?)
        :ret vector?
        :fn (fn [{{left :left right :right} :args ret :ret}]
              (= left (first ret))
              (= right (second ret))))

(defn pair-builder [x y]
  (vector x y))

;; Exercise with: (s/exercise-fn 'fake.flowrs/pair-builder)
;; Check with: (stest/check 'fake.flowrs/pair-builder)

;;; Another example

;; The following function is more like a FloWr node, insofar as
;; it returns a map, and the elements of the map could be thought of
;; as "fields".

;; Hm, I wonder if the function could actually be generated directly
;; from the spec?

;; I've asked a question about this function here:
;; https://groups.google.com/forum/#!topic/clojure/7qQJD3805I8

;; http://blog.cognitect.com/blog/2016/10/5/interactive-development-with-clojurespec
;; begins to address the issues there

;; I created an issue in the FloWrTester repo to keep track of
;; further discussion about this matter: in short, even though
;; we have a logical specification of the function behavior in
;; this case, generating the code from the spec would take some
;; more work.  It's interesting to wonder whether we could do
;; that with the generators in clojure.spec itself!

(s/fdef mapper
        :args (s/cat :t :fake.flowrs-tests/positive-integer?
                     :b :fake.flowrs-tests/positive-integer?)
        :ret map?
        :fn (fn [{args :args ret :ret}]
              (= (:tacos ret)
                 (vec (range (:t args))))
              (= (:burritos ret)
                 (vec (range (:b args))))))

;; Here's the desired definition:
(defn mapper [x y]
  {:tacos (vec (range x))
   :burritos (vec (range  y))})

;; This alternative definition kicks an error for `stest/check`:
;; (defn mapper [x y]
;;   {:tacos (vec (range x))
;;    :burritos (vec (range (+ 1  y)))})

;;; Some Wordnet-based examples

;; This is less developed, but in the other file I specified the *types*
;; Here I am specifying some functions that have those types for inputs
;; and outputs. -- E.g. this takes a "noun" as input and produces
;; "semicolon-separated-phrases" as output
(s/fdef gloss-noun
        :args :fake.flowrs-tests/noun?
        :ret :fake.flowrs-tests/semicolon-separated-phrases?)

(defn gloss-noun [x]
  (:gloss (first (flowrweb.core/wordnet x :noun))))

(s/fdef gloss-verb
        :args :fake.flowrs-tests/verb?
        :ret :fake.flowrs-tests/semicolon-separated-phrases?)

(defn gloss-verb [x]
  (:gloss (first (flowrweb.core/wordnet x :verb))))

(s/fdef semicolon-separated-phrases-to-vector
        :args :fake.flowrs-tests/semicolon-separated-phrases?
        :ret vector?)

(defn semicolon-separated-phrases-to-vector [x]
  ;; clean up on either side of the split
  (map str/trim (str/split x #";")))

;;; This part specifies the "spec" -- ...
(s/fdef phrase-to-glosses
        :args :fake.flowrs-tests/phrase?
        :ret vector?)

;; Tokenize to get the word senses, then look up glosses
(defn phrase-to-glosses [x]
  (into []
        (remove nil?
                (map
                 (fn [[word sense]]
                   (cond
                     (str/starts-with? sense "N") (gloss-noun word)
                     (str/starts-with? sense "V") (gloss-verb word)))
                 (flowrweb.core/pos-tag
                  (flowrweb.core/tokenize x))))))

;;; More stuff

;; Here it may make sense to write some functions for interacting with
;; Stephen's model, e.g. using the `gljcon` function from utility.clj
