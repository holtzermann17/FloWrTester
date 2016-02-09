;;; Local data storage

;; Note: from a design standpoint these are truly global variables, see notes below.

(def node-store (atom #{}))
(def user-charts (atom #{}))
(def type-store (atom #{}))

;; Every "agent" in the Clojure world will be associated with one
;; FloWr backend, and one FloWr "user".

;;; Initialisation

;; It is useful to have this separated out, otherwise we run into
;; trouble when loading the code w/o an internet connection.
;; (Most functions will give an error if not online.  It would
;; probably be useful to have a spoofed "upstream backend" for testing
;; but that's quite a bit of work.)

(defn init-local 
  "Reset the local cached information about nodes, charts, and node type data."
  []
  (binding [*print-length* 3]
    (reset! node-store (list-all-nodes))
    (reset! user-charts  (list-user-charts))
    (reset! type-store (map #(let [nodetype (:TYPE %)]
                               (conj (node-type-info nodetype)
                                     [:TYPE nodetype]))
                            @node-store))))

(defn save-local 
  "Spit the data into a local file."
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

;;; Querying the local info store

;; I think this should be exactly the nodes that are in the "retriever" name space.
;; Am I right about that?  If so, the API information needs updating.

(defn chart-starting-nodes
  "List the names of nodes that the API has indicated to be valid for use when starting a chart."
  []
  (let [nodes @node-store]
    (map #(first %)
         (filter #(= (second %) true)
                 (map vector (map :TYPE nodes) (map :CANSTART nodes))))))

(defn all-type-info-for-type
  "List the all of the node type info for this node type.
Output is similar to that obtained from node-type-info, but
this function uses the local information store rather than
querying the server."
  [type]
  (first (filter #(= (:TYPE %) type) @type-store)))

;; available outputs, e.g. for a type like "text.categorisers.SentimentCategoriser"
(defn available-outputs-for-type
  "List the output types that the valid for this node type."
  [type]
  (:OUTPUT (first (filter #(= (:TYPE %) type)
                          @type-store))))

(defn available-inputs-for-type
  "List the input types that the valid for this node."
  [type]
  ;; this restricts to just the basic elements
  ;; in general, we will want to incorporate a bit more information, e.g. the :ISDATE and :ISNUMERIC fields.
  ;; ... but at the moment there's more information there than we can really use, e.g.
  ;;    :DESCRIPTION is pretty useless for a computer, even if it is useful for humans
  ;; and furthermore
  ;;    :ALLOWMULTIOPTIONS is a sort of confusing (especially since most of the time it is set to false)
  (map #(select-keys % [:OPTIONS :TYPE :NAME])
       (:PARAMETERS (first (filter #(= (:TYPE %) type)
                                   @type-store)))))

;; Nodes of interest:

;; ConceptNet ConceptNetChainCategoriser ConceptNetChainSorter
;; ConceptNetChainer Dictionary FootprintMatcher Guardian LineCollator
;; ListAppender MatchingFootprintCategoriser MetaphorEyes
;; RegexCategoriser RegexGenerator RegexPhraseExtractor RhymeMatcher
;; RhymingMatcher SentimentCategoriser SimileRetriever TemplateCombiner
;; TextOverlapper TextRankKeyphraseExtractor TuplesAppender Twitter
;; WordListCategoriser WordReplacer WordSenseCategoriser

;;; Further Functionality

(defn delete-server-user-charts 
  "Delete user charts from the server and reset the local store.
Destructive of server state, use with caution."
  []
  (domap #'delete-chart @user-charts)
  (reset! user-charts #{}))

;; Note that the timestamp might be a second or so off from what's stored on the server
(defn add-to-local-user-charts
  "Insert a reasonable representation of a new chart CID to the user-charts variable."
  [cid]
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

;; Note, this of course needs internet access in order to run.

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

;; test code:
;; (script-to-flowchart "text.retrievers.ConceptNet.ConceptNet_0\ndataFile:simple_concept_net_1p0_sorted.csv\nlhsQuery:\nlhsQueries:\nrelation:IsA\nrhsQuery:profession\nrhsQueries:\nminScore:1.0\n#wordsOfType = answers[*]\n\ntext.categorisers.WordListCategoriser.WordListCategoriser_0\nwordList:\nwordListFileName:not_animals.txt\nstringsToCategorise:#wordsOfType\nstringArraysToCategorise:\nwordListArray:\npositionStringInArray:\n#filteredFacts = textsWithoutWord[*]")

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

;; Note: the following function is a first pass at extracting output from
;; the scripts after they are run.

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

;; Objectives: See 

;; This is an example of a node's self-description.  Content is from
;;  (pprint (node-type-info "text.retrievers.ConceptNet"))
(comment 

{:OUTPUT
 [{:SIMPLETYPE "ConceptNetOutput",
   :TYPE
   "ccg.flow.processnodes.text.retrievers.ConceptNet.ConceptNetOutput",
   :FIELDS
   [{:SIMPLETYPE "ArrayList<String>",
     :NAME "answers",
     :TYPE "java.util.ArrayList<java.lang.String>",
     :DESC ""}
    {:SIMPLETYPE "ArrayList<String[]>",
     :NAME "facts",
     :TYPE "java.util.ArrayList<java.lang.String[]>",
     :DESC ""}
    {:SIMPLETYPE "String",
     :NAME "theme",
     :TYPE "java.lang.String",
     :DESC ""}]}],
 :FG "#ffffff",
 :BG "#006400",
 :CANSTARTCHARTS true,
 :DESCRIPTION "Query the ConceptNet tuple-store.",
 :PARAMETERS
 [{:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "Single word or short underscore-separated phrase intended to match the left-hand term of some ConceptNet relations.",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS [],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "lhsQuery",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "A required threshold of confidence, checked against scores stored in the ConceptNet database.",
   :EDITOR "text",
   :MAXIMUM "10.0",
   :TYPE "float",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "float",
   :OPTIONS
   ["0"
    "0.5"
    "1.0"
    "1.5"
    "2.0"
    "3.0"
    "4.0"
    "5.0"
    "6.0"
    "7.0"
    "8.0"
    "9.0"
    "10.0"],
   :EDITOROPTIONS ["text"],
   :NAME "minScore",
   :MINIMUM "0.0",
   :DEFAULT "10",
   :OPTIONDISPLAYS [],
   :ISNUMERIC true}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "Single words or short underscore-separated phrases intended to match the right-hand term of some ConceptNet relations.",
   :EDITOR "text",
   :TYPE "java.util.ArrayList<java.lang.String>",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "ArrayList<String>",
   :OPTIONS [],
   :EDITOROPTIONS ["text"],
   :NAME "rhsQueries",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "A list of tuples. The element of each tuple at positionInTuple is added either to the left-hand side or right-hand side of the query, depending on the value of positionInQuery.",
   :EDITOR "text",
   :TYPE "java.util.ArrayList<java.lang.String[]>",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "ArrayList<String[]>",
   :OPTIONS [],
   :EDITOROPTIONS ["text"],
   :NAME "queries",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "Store elements sampled from the queries variable (if provided) in the left or right side of the query, depending on the value of this variable.",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS ["left" "right"],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "positionInQuery",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "One of a constrained set of subject-relation-object relations used by ConceptNet.",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS
   ["Any"
    "Antonym"
    "AtLocation"
    "Attribute"
    "CapableOf"
    "Causes"
    "CausesDesire"
    "ConceptuallyRelatedTo"
    "CreatedBy"
    "DefinedAs"
    "Derivative"
    "DerivedFrom"
    "DesireOf"
    "Desires"
    "Entails"
    "HasA"
    "HasContext"
    "HasFirstSubevent"
    "HasLastSubevent"
    "HasPainCharacter"
    "HasPainIntensity"
    "HasPrerequisite"
    "HasProperty"
    "HasSubevent"
    "InheritsFrom"
    "InstanceOf"
    "IsA"
    "LocatedNear"
    "LocationOfAction"
    "MadeOf"
    "MemberOf"
    "MotivatedByGoal"
    "NotCapableOf"
    "NotCauses"
    "NotDesires"
    "NotHasA"
    "NotHasProperty"
    "NotIsA"
    "NotMadeOf"
    "NotUsedFor"
    "ObstructedBy"
    "PartOf"
    "ReceivesAction"
    "RelatedTo"
    "SimilarSize"
    "SimilarTo"
    "SymbolOf"
    "Synonym"
    "UsedFor"],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "relation",
   :DEFAULT "IsA",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "Name of file to save facts in using Prolog format for use by HR3",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS [],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "hr3FileName",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION "Point to directory containing the file hr3FileName",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS [],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "hr3Dir",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "If queries, a list of tuples, is given, then the element of each tuple at positionInTuple is added either to the left-hand side or right-hand side of the query, depending on value of positionInQuery.\n\nMust be less than the length of the tuple in queries.",
   :EDITOR "text",
   :TYPE "int",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "int",
   :OPTIONS [],
   :EDITOROPTIONS ["text"],
   :NAME "positionInTuple",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC true}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION "Source of concept net relations",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS
   ["simple_concept_net_0p5_sorted.csv"
    "simple_concept_net_1p0_sorted.csv"
    "simple_concept_net_1p0_filtered.csv"],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "dataFile",
   :DEFAULT "simple_concept_net_0p5_sorted.csv",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "Single words or short underscore-separated phrases intended to match the left-hand term of some ConceptNet relations.",
   :EDITOR "text",
   :TYPE "java.util.ArrayList<java.lang.String>",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "ArrayList<String>",
   :OPTIONS [],
   :EDITOROPTIONS ["text"],
   :NAME "lhsQueries",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "Single word or short underscore-separated phrase intended to match the left-hand term of some ConceptNet relations.",
   :EDITOR "text",
   :TYPE "java.lang.String",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "String",
   :OPTIONS [],
   :EDITOROPTIONS ["textarea" "text"],
   :NAME "rhsQuery",
   :DEFAULT "",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}
  {:ALLOWMULTIOPTIONS false,
   :ISDATE false,
   :DESCRIPTION
   "in case the query as given returns no results, if splitWord is true, the process will re-run with altered inputs; namely, the process will break query strings on an underscore, and rerun the query with the first component.",
   :EDITOR "checkbox",
   :TYPE "boolean",
   :MULTIOPTIONSDELIMITER " ",
   :SIMPLETYPE "boolean",
   :OPTIONS ["true" "false"],
   :EDITOROPTIONS ["checkbox"],
   :NAME "splitWord",
   :DEFAULT "false",
   :OPTIONDISPLAYS [],
   :ISNUMERIC false}],
 :OUTPUTDESCRIPTION ""}
)
