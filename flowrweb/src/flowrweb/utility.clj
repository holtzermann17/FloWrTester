;;; General purpose functions

;; From https://gist.github.com/mattrepl/89249
(defn domap
  "A map for side-effects.  The argument order is the same as map, but
unlike map the function results are not retained.  Takes a function
followed by any number of collections and applies the function to the
first item in each coll, then the second etc.  Returns nil."
  [fn & colls]
  (let [num-colls (count colls)]
    (doseq [args (partition num-colls (apply interleave colls))]
      (apply fn args))))

(defn seq-contains?
  "Check whether the collection COLL contains an instance of TARGET."
  [coll target]
  (some #(= target %) coll))

(defn which [pred coll]
  (first (filter pred coll)))

;;; Some experiments with macros

;; Clojure doesn't permit you to `apply' a macro so here's
;; a work around.
;;
;; ... except, it doesn't work the way I was hoping.
;;
;; Some further experiments may be in order.

(defmacro functionize [macro]
  `(fn [& args#] (eval (cons '~macro args#))))

(defmacro apply-macro [macro args]
  `(apply (functionize ~macro) ~args))

;; https://groups.google.com/forum/#!topic/clojure/Ejia-zidVVc
(defn spread
  [arglist]
  (cond
   (nil? arglist) nil
   (nil? (next arglist)) (seq (first arglist))
   :else (cons (first arglist) (spread (next arglist)))))

(defmacro apply-macro-1
  "This is evil.  Don't ever use it.  It makes a macro behave like a
  function.  Seriously, how messed up is that?
  Evaluates all args, then uses them as arguments to the macro as with
  apply.
  (def things [true true false])
  (apply-macro and things)
  ;; Expands to:  (and true true false)"
  [macro & args]
  (cons macro (spread (map eval args))))

;;; For interacting with word2vec models

;; For the following, look for the source files in
;; ../src/incanter/modules/incanter-core/src/incanter
;; Cf. http://repo.incanter.org/
;;
(defn cosine-sim
  [model word1 word2]
  (let [rawvecfn  #(.getRawVector (.forSearch model) %)
        [a1 a2] (map rawvecfn [word1 word2])]
    (stats/cosine-similarity a1 a2)))

;; https://gist.github.com/paxan/9017097
;; This macro is useful for reading a list of sentences from a file
(defmacro with-open-seq
  "Like with-open, but only closes resources when the sequence generated by the body is exhausted."
  [bindings & body]
  `(let [c# (chan)]
     (go
       (try
         (with-open ~bindings
           (doseq [i# (do ~@body)]
             (>! c# (list i#))))
         (catch Exception e#
           (>! c# e#))
         (finally
           (close! c#))))
     (map first
          (take-while (complement nil?)
                      (repeatedly
                       #(let [i# (<!! c#)]
                          (if (instance? Exception i#)
                            (throw i#)
                            i#)))))))
