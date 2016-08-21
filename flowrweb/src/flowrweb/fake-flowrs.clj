(ns fake.flowrs
  (:require [clojure.string :as str]))

;; This function illustrates that there's a lot of things that
;; can be asserted even about a simple function.  Do we need
;; to assert all of this?  It's only relevant as long as it's 
;; interpretable.

(defn pair-builder 
  "Given two integers, return them formatted as a vector pair."
  [x y] 
  {:pre [(integer? x)
         (integer? y)]
   :post [(vector? %)
          (= x (nth % 0))
          (= y (nth % 1))
          (= (count %) 2)]
   }
  (let [y (+ 1 x)]
    [x y]))

;; The following function is more like a FloWr node, insofar as it returns
;; a map, and the elements of the map can be thought of as "fields".

(defn mapper [x y] 
  {:pre [(integer? x)
         (integer? y)]
   :post [(fake.flowrs-tests/test-each-one integer? (:tacos %))
          (fake.flowrs-tests/test-each-one integer? (:burritos %))]
   }
  {:tacos (vec (range x))
   :burritos (vec (range y))})

;; What to do next?

;; [DONE] Maybe what's really needed here is a collection of predicates
;; similar to the ones that I wrote out for existing FloWr nodes,
;; so that instead of writing `vector?` here, one would write
;; `vector-of-integers?` for example. 
;; [SEE fake-flowrs-tests, AND NOTICE THE IMPLEMENTATION OF `mapper`
;;  USES THE NEW FUNCTIONS.]
;;
;; With this way of thinking, what is interesting is *defining*
;; and *reasoning about* all of the different predicates that can describe
;; emitted or accepted variables, since these are also delimit the ways in
;; which functions can be hooked together.
;;
;; (But it is also worth thinking some more functions that are worth
;; defining!)

;; - Maybe some functions to use wordnet, e.g. `hyponym` and `hypernym`?
;; - Maybe get word2vec or GloVe running here and use that?
;;   cf. https://github.com/Bridgei2i/clojure-word2vec
;; - Maybe look through the Corneli & Corneli paper for ideas on
;;   "the composition problem"?
;; - There are some similar ideas in Chiriță and Fiadeiro -- 
;;   the models in this paper are a little bit like "templates" in the
;;   poetry paper

;; Here, notice that the tests describe the "shape" of the returned data,
;; so that subsequent clients can know how to disassemble them
;;
(defn gloss-noun [x]
  {:pre [(fake.flowrs-tests/noun? x)]
   :post [(fake.flowrs-tests/test-string-semicolon-separated-phrases  %)]}
   (:gloss (first (flowrweb.core/wordnet x :noun))))

(defn gloss-verb [x]
  {:pre [(fake.flowrs-tests/verb? x)]
   :post [(fake.flowrs-tests/test-string-semicolon-separated-phrases  %)]}
   (:gloss (first (flowrweb.core/wordnet x :verb))))

(defn semicolon-separated-phrases-to-vector [x] 
  {:pre [(fake.flowrs-tests/test-string-semicolon-separated-phrases x)]
   :post [(vector? %)]}
  ;; should clean up on either side of the split
  (str/split x #";"))

(defn phrase-to-glosses [x] 
  {:pre [(fake.flowrs-tests/test-string-is-phrase x)]
   :post [(vector? %)]}
  ;; here it would make sense to parse in order to get the word senses
  ;; chosen correctly, so we can convert to 
  (map (fn [word] )
       (str/split x #" ")))
