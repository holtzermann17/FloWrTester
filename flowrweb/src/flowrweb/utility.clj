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

;; Clojure doesn't permit you to `apply' a macro so here's a quick work around.
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

;; For the following,
;; look in /home/joe/FloWrTester/flowrweb/src/incanter/modules/incanter-core/src/incanter for the source files
;; Cf. http://repo.incanter.org/
;;
(defn cosine-sim
  [model word1 word2]
  (let [rawvecfn  #(.getRawVector (.forSearch model) %)
        [a1 a2] (map rawvecfn [word1 word2])]
    (stats/cosine-similarity a1 a2)))

;;; For interacting with Stephen's model

;;; Stuff for interacting with Stephen's model -- this could go into utility.clj instead

;; Need to check the details, but I think it will basically work.
;; I think it makes sense to start the system right away.
;; If we feed it, then we'll see the history of output.
(def sh-python (sh/proc "python" "-i" "/home/joe/Stephen/model/projecter" :dir "/home/joe/Stephen/model")) 
(def a-stringwriter (agent (java.io.StringWriter.)))
(future (sh/stream-to sh-python :out @a-stringwriter))

(defn feed-python [user-input & {:keys [delay] :or {delay 5000}}]
  (future (sh/feed-from-string sh-python (str user-input "\n"))
          ;; adjust the timeout according to whatever's realistic for python
          (Thread/sleep delay)
          (str/split (.toString @a-stringwriter) #"\n")))

;; Typically we're only interested in the last link
(defn gljcon [user-input & {:keys [delay] :or {delay 5000}}]
  (last @(feed-python user-input :delay delay)))

;; Sent RET to underlying process.
(defn gljcon-enter []
  (gljcon ""))

;; supply a word, possibly interactively
(defn gljcon-word 
  ([] (gljcon (do (print "word> ") (flush) (read-line))))
  ([word] (gljcon word)))

(defn gljcon-dimensions 
  ([] (gljcon "200"))
  ([dims] (gljcon (str dims))))

;; split the results up into words and triples
;; barked [52.935729506623403, 52.208653098872411, 48.176430454914382]
;; poodle [59.796810157374615, 51.799108148665077, 55.895011943121482]
;; etc
(defn gljcon-vectors 
  ([] (gljcon "20"))
  ([vecs] (gljcon (str vecs))))

(defn gljcon-analyze [& {:keys [vec] :or {vec 20}}]
  (let [ans (gljcon )
        by-distance (-> ans
                        (str/split #"BY NORM:")
                        (first)
                        (subs 25))
        by-norm (-> ans
                    (str/split #"BY NORM: |BY ANGLE:")
                    (second))
        ;; slightly idiosyncratic but there's one last link here to drop
        by-angle (-> ans
                     (str/split #"BY ANGLE: |OK\?:")
                     (second))
        parser (fn [segment]
                 (into {} (map (fn [portion]
                                 (let [item (str/split portion #" \[")
                                       word (first item)
                                       vec (str/split (second item) #", ")]
                                   {word, vec}))
                               (str/split segment #"\] "))))]
    ;; (assoc ret :by-distance (parser by-distance))
    ;; (assoc ret :by-norm (parser by-norm))
    ;; (assoc ret :by-angle (parser by-angle))
    (-> {} 
        (assoc :by-distance (parser by-distance))
        (assoc :by-norm (parser by-norm))
        (assoc :by-angle (parser by-angle)))))

;; The routine is basically:
;; loop as follows:
;; (gljcon-word) .. <- possibly several times
;; ...
;; (gljcon-enter)
;; (gljcon-dimensions)
;; (gljcon-vectors)
;; (gljcon-enter)

;; let's leave it at one loop for now, if we need to keep feeding more,
;; we can automate this in another function
(defn gljcon-interact [& words]
  (doseq [x words]
    (gljcon-word x))
  (gljcon-enter)
  (gljcon-dimensions)
  (let [res (gljcon-vectors)]
    (gljcon-enter)
    res))

