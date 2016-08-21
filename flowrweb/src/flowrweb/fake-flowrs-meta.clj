;;; A namespace for functions that interact with our database of "nodes"

(ns fake.flowrs-meta
  (:require [clojure.test :as test]
            [clojure.string :as str]))

;; Relevant theory -- program expressions in
;; Ionut Tutu & Fiadeiro, "Service Oriented Logic Programming"

;; First task: can we then search the defined functions for a given
;; property?

(defn queryfnx
  "Get the pre and post conditions for a given function.
The input should expressed in the form of a fully qualified var.
E.g. the fully qualified var that holds *this* function is #'fake.flowrs-meta/queryfnx."
  [var]
  (-> var meta :arglists first meta))

(defn all-prepost
  "Maps describing the functions in fake.flowrs with their :pre and :post conditions and :node name."
  []
  (map #(assoc (queryfnx (val %)) :node (key %))
       (ns-publics 'fake.flowrs)))

(defn all-prepost-tests
  "Return a lazy sequence of maps describing the functions in fake.flowrs-tests with all of their pre and post conditions."
  []
  (map #(assoc (queryfnx (val %)) :node (key %))
       (ns-publics 'fake.flowrs-tests)))

;; This should take into account the various available types/tests and subtypes
;; in order to make the most precise description of the input available.
;; So, for example, ["backpack"] specialises as "string->word->noun, verb"
;;         whereas  ["rucksack"] specialises as "string->word->noun"
;;
;; (Notice that there seems to be some theoretical structure here, regarding the creation of a new grammar.)

(def basic-tests
  "Define the basic types that all of the tests will ultimately be derived from."
  '[vector? integer? string? test/function? float?])

(defn heredity-of-signatures
  "Compute the derived type of each test."
  []
  (let [metadata-collection (all-prepost-tests)]
    (doseq [test metadata-collection]
      ;; for each individual test...
      (let [{pre :pre, node :node} test]
        (println node)
        ;; look at what defines its several preconditions...
        ;; and for each defining element, see if that is a "basic test"
        (let [match-results 
              (map #(let [yes 
                          ;; <idiom: like `some` but return the matching item
                          (first (filter (fn [basic-item] 
                                           (= basic-item (first %)))
                                         fake.flowrs-meta/basic-tests))]
                      ;; if we get a positive result, then this is very easy
                      (if yes
                        (str/upper-case (name yes))
                        ;; ... but if not, then we've found a derived test, and
                        ;; we need to trace its heredity
                        (cond
                          ;; CASE 1: `every?` will make us interested in the *second* place.
                          ;; ... but that's not quite enough, because we need to figure out which
                          ;; vector the `every?` command is associated with
                          (= (first %) 'every?) (str "every!->" (second %))
                          ;; CASE 2: otherwise the test should presumably be one of the defined (non-basic) tests
                          ;; ... but that one also may be defined in terms of another test, and so on...
                          ;; something to solve here! TODO
                          (some (fn [record] (= (first %)
                                                (:node record)))
                                metadata-collection) (str "test!->" (first %))
                          :else (first %))))
                                 pre)]
          (println " " match-results))))))


(defn dataset-profile
  "Code the data according to its type signature."
  [data]
  (into '[]
        (map (fn [x] (cond 
                       (integer? x) 'integer?
                       (vector? x)  'vector?
                       (symbol? x)  'symbol?
                       (string? x)  'string?
                       (test/function? x)  'function?
                       :else 'something-else))
             data)))

;; expand with :post information as well
;; - you know in advance that you want a castle
;; - or you have a loose specification, you want e.g. a "building kind of like that."
(defn signature-profile
  "Transform a function's `:pre` information to make it easy to compare with other data sources."
  [sig]
  (into '[]
        (map (fn [x] (cond
                       ;; Note, this is written in an unnecessarily complex way "on purpose,"
                       ;; -- as a reminder that we might eventually want
                       ;; to do more with signatures than just have
                       ;; types and simple predicates.
                       (= 'integer? (first x)) 'integer?
                       (= 'vector? (first x))  'vector?
                       (= 'symbol? (first x))  'symbol?
                       (= 'test/function? (first x))  'function?
                       :else (first x)))
             sig)))

;; Discovery part in service oriented computing, but you next have to decide
;; which ones go together.  You could have chosen any one of those that fit.
;; Claudia: "Many Valued Constraint Specification".  If all you know is that they fit, then you can't do
;; a lot -- but if you know how *well* they fit, then you can do more.
;;
;; The application can keep track of global optimisations -- it will impose some global optimisations.
;; Services have requirements but your goal is not to fulfill the services needs.  They can have requirements
;; that are triggered...
(defn applicable-services
  "Given that we have a vector of data, what services could be applied?"
  [data]
  ;; Sorting the data and the relevant :pre conditions seems to be the
  ;; straightfoward way to make them comparable.
  (let [data-profile (sort (dataset-profile data))
        matches (map :node
                     (filter #(= data-profile
                                 (sort (signature-profile (:pre %))))
                             (all-prepost)))]
    ;; some fuss here to return nil rather than an empty list
    (when (not (empty? matches))
      matches)))

;; ... But this doesn't take into consideration what the services return,
;; nor does it have a built in way to evaluate whether one of these
;; services is better than the other.

;; So, in principle we will have a bunch of functions, which have the following properties:

;; 1. We can match their inputs and outputs to assemble them,
;;    Lego-style, into larger programs.

;; 2. The programs should do something quite useful and adaptive, e.g. build a story.
;;    - But, since we don't have tremendously sensible background
;;      knowledge structures for building sensible stories, maybe
;;      we should be allow somewhat non-sensical stories to begin with?

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

