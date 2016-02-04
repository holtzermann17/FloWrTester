;;; extractor.clj

;;; Commentary:

;; This code implements the basic algorithm from:
;;
;; D. Rusu, L. Dali, B. Fortuna, M. Grobelnik, and D. Mladenic. 
;; Triplet extraction from sentences.
;; In Proceedings of the 10th International Multiconference Information Society-IS, pages 8–12, 2007.

;; The implementation uses Clojure's "zipper" functionality to
;; traverse parse trees.

;; Status: It works but could use some tuning.

;;; Code:

(defn sequence-to-vector
  "This turns a treebank parse tree into a vector, which makes it easier to manipulate with zippers."
  [treebank]
  (read-string
   (clojure.string/replace 
    ;; quote all words
    (clojure.string/replace treebank  #"[^() ]+" #(str "\"" %1 "\""))
    #"[()]" #(if (= %1 "(") "[" "]"))))

;;;;;;;;;;;;;;;
;; Some matchers

(defn match-np? [loc]
  (= "NP" (z/node loc)))

(defn match-noun? [loc]
  (some #{"NN" "NNP" "NNPS" "NNS"} (z/node loc)))

(defn match-vp? [loc]
  (= "VP" (z/node loc)))

(defn match-pp? [loc]
  (= "PP" (z/node loc)))

(defn match-verb? [loc]
  (some #{"VB" "VBD" "VBG" "VBN" "VBZ"} (z/node loc)))

(defn match-adjp? [loc]
  (= "ADJP" (z/node loc)))

(defn match-adjective? [loc]
  (some #{"JJ" "JJR"} (z/node loc)))

;;;;;;;;;;;;;;;

;; Basic working finder - this is actually used
(defn tree-find
  "Take a zipper and a function that matches a pattern in the tree.
  Examine the tree nodes in depth-first order, determine whether the
  matcher matches, and if so return the parent context."
  [zipper matcher]
  (loop [loc zipper]
    (if (z/end? loc)
      (z/root loc)
      (if-let [matcher-result (matcher loc)]
        (z/node (z/up loc))
        (recur (z/next loc))))))

;; helper function called by `tree-find-descendent', see below
(defn tree-find-this
  "Take a zipper and a function that matches a pattern in the tree.
  Examine the tree nodes in depth-first order, determine whether the
  matcher matches, and if so return it."
  [zipper matcher]
  (loop [loc zipper]
    (if (z/end? loc)
      (z/root loc)
      (if-let [matcher-result (matcher loc)]
        (z/node loc)
        (recur (z/next loc))))))

;; Find the first matching piece, then find its first matching descendent
(defn tree-find-descendent
  "Take a zipper, a function that matches a pattern in the tree, and another function that matches a descendent.
  Examine the tree nodes in depth-first order, determine whether the
  first matcher matches, and if so return the first descendent that matches the second predicate."
  [zipper matcher descendent]
  (loop [loc zipper]
    (if (z/end? loc)
      (z/root loc)
      (if-let [matcher-result (matcher loc)]
        (tree-find-this (z/up loc) descendent)
        (recur (z/next loc))))))

(defn tree-find-last
  "Take a zipper and a function that matches a pattern in the tree.
  Examine the tree nodes in depth-first order to amass a list of matching
  nodes, and return the last one."
  [loc matcher]
  (last 
   (filter matcher ;filter only matching nodes
           (take-while (complement z/end?) ;take until the :end
                       (iterate z/next loc)))))

(defn tree-find-last-descendent
  "Take a zipper, a function that matches a pattern in the tree, and another function that matches a descendent.
  Examine the tree nodes in depth-first order, determine whether the
  first matcher matches, and if so return the first descendent that matches the second predicate."
  [zipper matcher descendent]
  (loop [loc zipper]
    (if (z/end? loc)
      (z/root loc)
      (if-let [matcher-result (matcher loc)]
        (z/node (tree-find-last (z/up loc) descendent))
        (recur (z/next loc))))))

;; This simply finds the siblings
(defn tree-find-siblings
  "Find the siblings of loc."
  [loc]
  ;; scroll to the right -- it seems there's no "nice" way to add
  ;; the last element, so just tack it on at the end using a hack.
  ;; http://stackoverflow.com/questions/5734435/put-an-element-to-the-tail-of-a-collection
  (concat (take-while (complement #(= (z/right %) nil))
                      (iterate z/right (z/leftmost loc)))
          [(z/rightmost loc)]))

;; This involves horizontal motion so we need to make sure to get all of the horizontal items?
(defn tree-find-siblings-of-last-descendent
  "Take a zipper, a function that matches a pattern in the tree, and another function that matches a descendent.
  Examine the tree nodes in depth-first order, determine whether the
  first matcher matches, and if so return the first descendent that matches the second predicate."
  [zipper matcher descendent]
  (loop [loc zipper]
    (if (z/end? loc)
      (z/root loc)
      (if-let [matcher-result (matcher loc)]
        (let [the-sibling (tree-find-last (z/up loc) descendent)]
          (tree-find-siblings the-sibling)
          )
        (recur (z/next loc))))))

;; This function works efficiently because `filter' is lazy.
;; This is a cool example illustrating lazy evaluation!
(defn find-first
  [f coll]
  (first (filter f coll)))

;; The following functions do the basic extractions we're looking for.
;; At the moment I'm going to leave aside the question of building up attribute lists

(defn tree-find-subject [zipper]
  (tree-find-descendent zipper match-np? match-noun?))

(defn tree-find-predicate [zipper]
  (tree-find-last-descendent zipper match-vp? match-verb?))

;; This finder looks in the zipper for the verb phrase, finds its
;; siblings, and among those, locates the nouns.  It then finds the
;; first actual noun from the matched siblings, and returns that.
(defn tree-find-object [zipper]
  ;; find the siblings of the verb 
  (let [siblings (tree-find-siblings-of-last-descendent zipper match-vp? match-verb?)]
    ;; take the first noun we find
    (or 
     (find-first #(contains? #{"NN" "NNP" "NNPS" "NNS"} (first %))
                 ;; among the siblings, look for either a NP or a PP containing any noun
                 (map #(tree-find-descendent %
                                             (fn [x] (or (match-np? x)
                                                         (match-pp? x)))
                                             match-noun?)
                      siblings))
     ;; if we don't find any noun, take the first adjective we find
     (find-first #(contains? #{"JJ" "JJR"} (first %))
                 ;; among the siblings, for either an ADJP containing an adjective
                 (map #(tree-find-descendent %
                                             match-adjp?
                                             match-adjective?)
                      siblings)))))



;;;;;;;;;;;;;;;

;; SENTENCE comes from an opennlp treebank parse
(defn triplet-extraction [sentence]
  (let [svector (sequence-to-vector sentence)
        szipper (z/zipper vector? seq (fn [_ c] c) svector)
        ;; candidate as a vector
        candidate [(tree-find-subject szipper)
                   (tree-find-predicate szipper)
                   (tree-find-object szipper)]]
    (if (some nil? candidate)
      false
      candidate)))

;;; Example of reading from a file

(defn read-frankenstein [number-of-sentences]
  (with-open-seq [rdr (clojure.java.io/reader "/home/joe/FloWrTester/corpus/frankenstein.txt")]
    (let [sentences (sentence-seq rdr get-sentences)
          lsentences (take number-of-sentences sentences)
          lparse (map #(first (treebank-parser
                               ;; requires a vector input
                               [(clojure.string/join " " %)]))
                      (lazy-tokenize
                       lsentences
                       tokenize))]
      ;; process your lazy seq of sentences however you desire
      (println "first number-of-sentences sentences:")
      (map (fn [sent]
             (clojure.pprint/pprint sent)
             (try (do (print "Extraction: ")
                      (clojure.pprint/pprint (triplet-extraction sent)))
                  (catch Exception e
                    (println "EXCEPTION: " (.getMessage e)))))
           lparse))))


;; function TRIPLET-EXTRACTION(sentence)
;; "returns a solution, or failure"
;;     result ← EXTRACT-SUBJECT(NP_subtree) + EXTRACT-PREDICATE(VP_subtree) + EXTRACT-OBJECT(VP_siblings)
;;     if result ≠ failure then return result
;;     else return failure

;; function EXTRACT-ATTRIBUTES(word)
;; "returns a solution, or failure"
;; # search among the word’s siblings
;; if adjective(word)
;;     result ← all RB siblings
;; else
;; if noun(word)
;;     result ← all DT, PRP$, POS, JJ, CD, ADJP, QP, NP siblings
;; else
;; if verb(word)
;;     result ← all ADVP siblings
;; # search among the word’s uncles
;; if noun(word) or adjective(word)
;;     if uncle = PP
;;         result ← uncle subtree
;; else
;;     if verb(word) and (uncle = verb)
;;         result ← uncle subtree
;; if result ≠ failure then return result
;; else return failure

;; function EXTRACT-SUBJECT(NP_subtree)
;; "returns a solution, or failure"
;; subject ← first noun found in NP_subtree
;; subjectAttributes ← EXTRACT-ATTRIBUTES(subject)
;; result ← subject ∪ subjectAttributes
;; if result ≠ failure then return result
;; else return failure

;; function EXTRACT-PREDICATE(VP_subtree)
;; "returns solution, or failure"
;; predicate ← deepest verb found in VP_subtree
;; predicateAttributes ← EXTRACT-ATTRIBUTES(predicate)
;; result ← predicate ∪ predicateAttributes
;; if result ≠ failure then return result
;; else return failure

;; function EXTRACT-OBJECT(VP_sbtree)
;; "returns a solution, or failure"
;; siblings ← find NP, PP and ADJP siblings of
;; VP_subtree
;; for each value in siblings do
;;     if value = NP or PP
;;         object ← first noun in value
;;     else
;;         object ← first adjective in value
;;         objectAttributes ← EXTRACT-ATTRIBUTES(object)
;; result ← object ∪ objectAttributes
;; if result ≠ failure then return result
;; else return failure

;; Figure 1: The algorithm for extracting triplets in
;; treebank output.

;;; extractor.clj ends here
