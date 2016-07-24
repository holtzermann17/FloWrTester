;;; Commentary:

;; Defines functions for automatic flowchart assembly.
;; Depends on the API wrapper in flowrweb-api.clj.

;; Possible improvements:

;; - Most functions will give an error if not online.  It would
;;   probably be useful to have a spoofed "upstream backend" for
;;   testing but that's quite a bit of work.

;;; Code:

;;; 0. Global variables (storage)

;; These can be populated with data provided over the API about
;; available nodes and charts.

;; (From a design standpoint these are truly global variables.
;; Multiple Clojure "agents" will connect to the server via one
;; FloWr "user".  More refined details below.)

(def node-store (atom #{}))
(def user-charts (atom #{}))
(def type-store (atom #{}))

(def local-charts (atom #{}))

;;; 1. Initialisation, caching, restoring

(defn init-local 
  "Reset the local cached information about nodes, charts, and node type data to match the server."
  []
  (binding [*print-length* 3]
    (reset! node-store (list-all-nodes))
    (reset! user-charts  (list-user-charts))
    (reset! type-store (map #(let [nodetype (:TYPE %)]
                               (conj (node-type-info nodetype)
                                     [:TYPE nodetype]))
                            @node-store))))

(defn save-local
  "Spit the local data store into a file for offline storage."
  []
  (binding [*print-length* nil]
    (spit "flowr-data.clj" (prn-str @node-store))
    (spit "flowr-data.clj" (prn-str @user-charts) :append true)
    (spit "flowr-data.clj" (prn-str @type-store) :append true)))

(defn reinit-local 
  "Initialise the local variables from information stored in a file.
This avoids pinging the API."
  []
  (let [data (str/split-lines (slurp "flowr-data.clj"))]
    (reset! node-store (edn/read-string (nth data 0)))
    (reset! user-charts (edn/read-string (nth data 1)))
    (reset! type-store (edn/read-string (nth data 2)))))

;;; 2. Querying the local info store

(defn all-node-names
  "List the names of all of the nodes."
  []
  (let [nodes @node-store]
    (map :TYPE nodes)))

(defn chart-starting-nodes
  "List the names of nodes that the API has indicated to be valid for use when starting a chart."
  []
  ;; Note: I think this should be exactly the nodes that are in
  ;; the "retriever" name space. Am I right about that?  If so, the
  ;; API information probably needs updating.
  (let [nodes @node-store]
    (map #(first %)
         (filter #(= (second %) true)
                 (map vector (map :TYPE nodes) (map :CANSTART nodes))))))

(defn all-type-info-for-type
  "List all of the node type info for this node type.
Output is similar to that obtained from node-type-info, but this
function uses the local information store rather than querying the
server."
  [type]
  (first (filter #(= (:TYPE %) type) @type-store)))

(defn available-outputs-for-type
  "List the output types that the valid for this node type.
Input is a type like \"text.categorisers.SentimentCategoriser\"."
  [type]
  (:OUTPUT (first (filter #(= (:TYPE %) type)
                          @type-store))))

(defn available-inputs-for-type
  "List the input types that the valid for this node.
Input is a type like \"text.categorisers.SentimentCategoriser\"."
  [type]
  ;; For now this restricts to just the basic description of parameters.
  ;; in general, we would want to incorporate a bit more information, e.g. the :ISDATE and :ISNUMERIC fields
  ;; give more specific type information than what's provided by the :TYPE keyword.
  ;; There's some additional information that we can't really use, e.g.
  ;;    :DESCRIPTION is pretty useless for a computer, even if it is useful for humans
  ;; and furthermore
  ;;    :ALLOWMULTIOPTIONS is a sort of confusing (especially since most of the time it is set to false)
  (map #(select-keys % [:OPTIONS :TYPE :NAME])
       (:PARAMETERS (first (filter #(= (:TYPE %) type)
                                   @type-store)))))

(defn get-latest-chart
  "Return the chart in user-charts with the highest cid."
  []
  (last (sort-by :CID @user-charts)))

(defn get-terminal-nodes 
  "Get the last node from chart CID by following the arrows.
This returns a list, since there may be multiple \"last\" nodes;
however, in practice there will typically only be one \"last\"."
  [cid]
  (let [chart (get-chart cid)
        ;; all the arrows
        arrows (:ARROWS (:CHART chart))
        ;; all the tails
        tails (distinct (map (fn [arrow] (second arrow))
                                       arrows))]
    ;; just the tails that ARE NOT equal to some head
    ;; (These are the tails from which no further progress is made in the chart.)
    (filter #(not (some (fn [arrow]
                          (= %
                             (first arrow)))
                        arrows))
            tails)))

;;; 2a. Find the potential connections between different kinds of nodes

;; E.g. ConceptNet's `answers`[*] output values can be fed into (another) ConceptNet's `lhsQueries` parameter
;;
;; Implementation notes:
;;
;; - This basically reproduces the "click-and-drag-into-empty-space" feature from the FloWr web interface,
;;   which is presumably useful to have around.
;;
;; - Keep in mind that the reasoning here is relative to "node-types",
;;   not to instantiated nodes (indeed, it's not clear that this matters
;;   in the current FloWr implementation).
;;
;; - Basic idea: for each :OUTPUT from node A, recursively look at each of the fields
;;   -- compare its :TYPE (and any "extended type" information of interest)
;;           to the :TYPE and :OPTIONS of each parameter from B
;;    -- if there is a match, record that.
;;
;; NB. We can potentially build up the database of "extended type" information *empirically*
;; drawing on any errors that FloWr returns when we try to actually establish a connection,
;; though that seems like going about things the hard way.

;;; `potential-connections`: can match on type, should consider more - June 23, 2016

;; Note: The way things are stored in `a-outputs` structure is
;;  [{:SIMPLETYPE "Foo",
;;     :FIELDS [{:SIMPLETYPE "Bar",...}
;;              {:SIMPLETYPE "Baz",...}]}
;;   {:SIMPLETYPE "Qux",
;;     :FIELDS [{:SIMPLETYPE "Fred",...}
;;              {:SIMPLETYPE "Barney",...}]}
;;   ...]
;; 
;; -- Furthermore, it looks like these lists are built so that they stay basically flat.
;;    E.g. if Baz, for example, has subfields, then these will be defined in a subsequent
;;    entry at the top level.

(defn potential-connections
  "List the possible connections between a node of type A and a node of type B.
At the moment this looks through the fields of A, and finds inputs of
B with matching type."
;; A future version of this function should take into account any
;; further constraints that we might know about A or B.  For example,
;; B sometimes has "options" and, at least in principle, A may
;; have "extended type" information (e.g. we might know that it is a
;; date, not just a string).
  [a b]
  (let [a-outputs (apply concat (map (fn[x] (map #(select-keys % [:NAME :TYPE]) x))
                                     (map :FIELDS (available-outputs-for-type a))))
        b-inputs (map #(clojure.set/rename-keys (select-keys % [:NAME :TYPE])
                                                {:NAME :SINK})
                      (available-inputs-for-type b))]
    ;; match the types
    (apply concat
           (for [output a-outputs] 
             (let [matching-inputs (filter #(= (:TYPE output)
                                               (:TYPE %))
                                           b-inputs)]
               ;; add the name of the output to the corresponding matched data
               ;; so that the association is maintained.
               (map (fn[x] (assoc x :SOURCE (:NAME output))) matching-inputs)
               )))
    ))

;;; `potential-downstream` and `potential-upstream`

;; These use the function above as a subroutine

(defn potential-downstream
  "List the possible downstream recipient nodes for a source node of type A."
  [a]
  (filter #(not-empty (potential-connections a %))
   (map :TYPE @node-store)))

(defn potential-upstream
  "List the possible upstream source nodes for a node of type B."
  [b]
  (filter #(not-empty (potential-connections % b))
   (map :TYPE @node-store)))

(defn potential-downstream-fields
  "List all of the available downstream fields that a source node of type A could send output to."
  [a]
  (apply concat (for [down (potential-downstream a)]
                  (map (fn[x] (assoc x :DOWN down)) (potential-connections a down))
                  )))

(defn potential-upstream-fields
  "List all of the available upstream fields that could send input to the recipient node B."
  [b]
  (apply concat (for [up (potential-upstream b)]
                  (map (fn[x] (assoc x :UP up)) (potential-connections up b))
                  )))

;;; `potential-downstream-field-wizard` and `potential-upstream-field-wizard` - to write June 23, 2016
;; These variants zoom in on one field at a time, so we can look at ways to connect up specific
;; fields of interest

(defn potential-downstream-field-wizard
  "List all of the available downstream fields that could send receive input from field FIELD in source node A."
  [a field]
  )

(defn potential-upstream-field-wizard
  "List all of the available upstream fields that could send input to field FIELD in recipient node B."
  [b field]
  )

;;; 2. Create and operate on a local flowchart object

;; Define the object in a way that's similar to the contents of @user-charts?
;;  -- No, that's not good, for now because the user-charts are just a list of CIDs stored on the server.  
;;  -- but (get-chart <N>) will give you something worthwhile.

;;; 3. Operate on server

(defn delete-server-user-charts 
  "Delete user charts from the server and reset the local store.
Destructive of server state, use with caution."
  []
  (domap #'delete-chart @user-charts)
  (reset! user-charts #{}))

;;helper:
(defn add-to-local-user-charts
  "Insert a reasonable representation of a new chart CID to the user-charts variable."
  [cid]
  ;; Note following this implementation the timestamp might be a
  ;; second or so off from what's stored on the server, if that
  ;; matters we could adjust it with another query.
  (let [timestamp (tf/unparse timestamp-formatter (t/now))]
  (swap! user-charts conj {:OWNEDBYUSER 1,
                           :API 1,
                           :JSON nil,
                           :DESCRIPTION "",
                           :CID cid,
                           :LOCKED 0,
                           :OWNERNAME "Joe Corneli",
                           :PUBLIC 0,
                           :CREATED timestamp,
                           :OWNERID 3,
                           :WEB 0,
                           :NAME (str "chart " timestamp),
                           :EDITABLE 1,
                           :DELETABLE 1})))

(defn script-to-flowchart
  "Create a flowchart represented by SCRIPT on the server.
Return the new chart number (cid)."
  [script]
  (let [mycid (new-chart)
        stanzas (str/split script #"\n\n")]
    ;(println "Chart:" mycid)
    (doseq [stanza stanzas]
      (let [lines (str/split stanza #"\n")
            ;; The "type" of a node named "text.retrievers.ConceptNet.ConceptNet_0"
            ;; is "text.retrievers.ConceptNet".
            nodetype (str/join "." (butlast (str/split (first lines) #"\.")))
            nodename (add-node mycid nodetype)]
        ;(println "Type:" nodetype)
        ;(println "In Chart" mycid " created:" nodename)
        (doseq [line (rest lines)]
          ;; leading hash mark means add OUTPUT VARIABLE settings...
          (if (= (first line) \#)
            (let [[lhs rhs] (map #'str/trim (str/split line #"="))]
              (if (and lhs rhs)
                (do 
                  ;; (1) create a new variable, which initially has a generic name
                  ;; (2) rename the variable
                  (rename-variable mycid nodename (new-variable mycid nodename) lhs)
                  ;; (3) set the variable
                  (set-variable-definition mycid nodename lhs rhs)
                  ;(println "(Variable) Set" nodename lhs " to" rhs)
                  )))
            ;; ...whereas in the alternate case, we deal with INPUT PARAMETER settings
            (let [[lhs rhs] (str/split line #":")]
              (if (and lhs rhs)
                (do
                  (set-parameter mycid nodename lhs rhs)
                  ;(println "(Parameter) Set" nodename lhs " to" rhs)
                  )))))))
    (println mycid)
    (println (get-chart (Integer. mycid)))
    (add-to-local-user-charts (Integer. mycid))
    (Integer. mycid)))

;; Note: the following function is a first pass at creating a
;; flowchart from a script and extracting output all in one go.
;;; `send-and-run-script` NEEDS WORK: June 20, 2016

(defn send-and-run-script
  "Create a flowchart represented by SCRIPT on the server, and run it.
Return the output of the last node (or nodes). This returns a list,
since there may be multiple \"last\" nodes; however, in practice
there will typically only be one \"last\"."
  [script]
  (let [cid (script-to-flowchart script)]
    (run-chart cid)
    ;; it might be more convenient to return a hashmap:
    ;; key: name of terminal node
    ;; val: output from that node
    (map #(get-node-output cid %)
         (get-terminal-nodes cid))))

;;; 4. Testing

;; test code:
;; (script-to-flowchart "text.retrievers.ConceptNet.ConceptNet_0\ndataFile:simple_concept_net_1p0_sorted.csv\nlhsQuery:\nlhsQueries:\nrelation:IsA\nrhsQuery:profession\nrhsQueries:\nminScore:1.0\n#wordsOfType = answers[*]\n\ntext.categorisers.WordListCategoriser.WordListCategoriser_0\nwordList:\nwordListFileName:not_animals.txt\nstringsToCategorise:#wordsOfType\nstringArraysToCategorise:\nwordListArray:\npositionStringInArray:\n#filteredFacts = textsWithoutWord[*]")



