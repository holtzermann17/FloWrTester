;;; A namespace for functions that interact with our database of "nodes"

(ns fake.flowrs-meta
  (:require [clojure.test :as test]))

;; First task: can we then search the defined functions for a given
;; property?

(defn queryfnx
  "Get the pre and post conditions for a given function.
The input should expressed in the form of a fully qualified var.
E.g. the fully qualified var that holds *this* function is #'fake.flowrs-meta/queryfnx."
  [var]
  (-> var meta :arglists first meta))

(defn all-prepost
  "Return a lazy sequence of maps describing the functions in fake.flowrs with all of their pre and post conditions."
  []
  (map #(assoc (queryfnx (val %)) :node (key %))
       (ns-publics 'fake.flowrs)))

(defn dataset-profile
  [data]
  (into '[]
        (map (fn [x] (cond 
                       (integer? x) 'integer?
                       (vector? x)  'vector?
                       (symbol? x)  'symbol?
                       (test/function? x)  'function?
                       :else 'something-else))
             data)))

(defn signature-profile
  [sig]
  (into '[]
        (map (fn [x] (cond 
                       ;; This is written in an unnecessarily complex way,
                       ;; under the assumption that we might eventually want
                       ;; to do more with signatures than just have types and simple predicates
                       (= 'integer? (first x)) 'integer?
                       (= 'vector? (first x))  'vector?
                       (= 'symbol? (first x))  'symbol?
                       (= 'test/function? (first x))  'function?
                       :else (first x)))
             sig)))

(defn relevant-services
  "Given that we have a vector of data, what services could be applied?"
  [data]
  ;; Let's actually treat the data as a set, and test for
  ;; equality as set
  (map :node
       (filter #(= (into #{} (dataset-profile data))
                   (into #{} (signature-profile (:pre %)))) (all-prepost))))

;; So, in principle we will have a bunch of functions, which have the following properties:

;; 1. We can match their inputs and outputs to assemble them,
;;    Lego-style, into larger programs.
;; 2. The programs should do something quite useful, e.g. build a story.
;;    - But, since we don't have tremendously sensible background
;;      knowledge structures for building sensible stories, maybe
;;      we should be happy to build somewhat non-sensical stories.

;; Story-writing algorithm
;;
;; - decide what the story is about (pick a prompt)
;; - read the prompt and understand the constituent terms (build KB)
;; - find a path through the KB (A* algorithm?)
;; - 'narrate' the path (say what is encountered along the way)

;; Function-writing algorithm
;;
;; - decide what the function achieves (pick inputs and outputs)
;; - write functional transformations of inputs into outputs

;; Dialogue algorithm
;;
;; - listen to what the previous speaker said
;; - associate what they said to a existing knowledge base
;; - compose a response
;; - update the knowledge base with a reflection of the statement and response

; So, where does the automatic program generation stuff come into play
; in these various examples?

