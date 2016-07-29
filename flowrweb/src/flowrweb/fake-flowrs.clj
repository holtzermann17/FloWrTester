(ns fake.flowrs)

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
;; a map, and the elements of the map are like "fields".
;;
;; [DONE] Maybe what's really needed here is a collection of predicates
;; similar to the ones that I wrote out for existing FloWr nodes,
;; so that instead of writing `vector?` here, one would write
;; `vector-of-integers?` for example. [SEE ABOVE]
;;
;; With this way of thinking, what becomes interesting is *defining*
;; and *reasoning about* all of the different predicates that can describe
;; emitted or accepted variables, since these are also delimit the ways in
;; which functions can be hooked together.
;;
;; (But it is also worth thinking some more about the classes of functions
;; that are worth defining!)

(defn mapper [x y] 
  {:pre [(integer? x)
         (integer? y)]
   :post [(fake.flowrs-tests/test-each-one integer? (:tacos %))
          (fake.flowrs-tests/test-each-one integer? (:burritos %))]
   }
  {:tacos (vec (range x))
   :burritos (vec (range y))})

