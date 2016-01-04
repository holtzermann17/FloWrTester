;;; Commentary:


;;; Code:

;;; 0. Useful global variables

;; It seems useful to store charts as well as their output.
;; JAC: note, I think storing these things on refs makes sense,
;; I just need to remember how to read the values that are saved
;; on the refs later on.
(def flowrweb-url "http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/")
(def flowrweb-user "holtzermann17@gmail.com")
(def flowrweb-token "oziFZjAJHzAOxlFCn7N2f82yT1PdenkM")

;;; 1. Functions that integrate with the FloWrWeb API

;; we should be able to write a macro that creates all of the API
;; functions in one fell swoop.  It will take:
;; - function name
;; - doc string
;; - list of arguments
;; - an indication of whether or not to decode JSON.

;; We can build this up step by step.

(defn convert-map-to-url-string
  "Transform a map like {:a \"foo\", :b \"bar\"} to a string like \"a=foo&b=bar\"."
  [m]
  (str/join "&"
        (map (fn [[k v]] 
               (str (name k) "=" v))
             m)))

(defn post 
  "Send API COMMAND to the FloWr Web URL, and decide whether or not to DECODE-JSON depending on the value of that variable.
\"Optional\" arguments ARGS are those required by the COMMAND and should be fed in as a" 
  [command decode-json & {:keys [args]}]
  (let [data {:api_token flowrweb-token
              :api_email flowrweb-user
              :c command}
        resp (client/post flowrweb-url 
                          ;; Note, the FloWr web API does not currently accept JSON
                          ;; but rather a URL-parameter-like body.
                          {:body (convert-map-to-url-string (merge data
                                                                   args))
                           :content-type :json
                           :headers {"Accept-Charset" "UTF-8"
                                     "Content-Type" "application/x-www-form-urlencoded;charset=UTF-8"}
                           })]
    ;; we may or may not be getting valid JSON back from the server
    ;; luckily we know in advance what to do
    (if decode-json
      (parse-string (:body resp)
                    (fn [k] (keyword (.toUpperCase k))))
      (:body resp))))

;;; This commented-out testing code is superseded by `create-api-function' and calls to the same, below.

;; (defn test-access []
;;   (post "test_access" false))

;; (defn list-all-nodes []
;;   (post "list_all_nodes" true))

;; (defn node-type-info [type]
;;   (post "node_type_info" true :args {:type type}))

;; (defn new-chart []
;;   (post "new_chart" false))

;; (defn add-node [cid type]
;;   (post "add_node" false :args {:cid cid :type type}))

(defn convert-seq-to-map 
  "Convert sequence S into a map that redundantly names each element with a keyword.
For example (a b) becomes {:a a, :b b}.  This is useful for feeding URL parameters to an API."
  [s]
  (into {} (map vector 
                (map #(keyword %) s)
                s)))

;; define the function with a LISP-style name
(defmacro create-api-function [fnname decode-json & arguments]
  `(do (defn ~(symbol (str/replace (name fnname) "_" "-" ))
         [~@arguments]
         (post ~(name fnname) ~decode-json :args ~(convert-seq-to-map arguments)))))

;; Aside: you can't `apply' macros in Clojure, though there are probably some clever
;; Let Over Lambda style ways to make it work.  See `apply-macro' and
;;  `apply-macro-1' in core.clj. 

;; For now, the following doesn't take up too much space:

(create-api-function test_access             false)
(create-api-function list_all_nodes          true)
(create-api-function list_user_charts        true)
(create-api-function new_chart               false)
(create-api-function delete_chart            false cid)
(create-api-function add_node                false cid type)
(create-api-function delete_node             true  cid nid)
(create-api-function get_chart               true  cid)
(create-api-function clear_output            false cid)
(create-api-function run_chart               false cid)
(create-api-function run_status              true  cid)
(create-api-function get_parameters          true  cid nid)
(create-api-function set_parameter           false cid nid pname pvalue)
(create-api-function new_variable            false cid nid)
(create-api-function rename_variable         true  cid nid vname nname)
(create-api-function delete_variable         true  cid nid vname)
(create-api-function get_variables           true  cid nid)
(create-api-function get_output_tree         true  cid nid)
(create-api-function set_variable_definition true  cid nid vname vdef)
(create-api-function get_node_output         true  cid nid)
(create-api-function get_variable_output     true  cid vname)
(create-api-function node_type_info          true  type)

;; And with that, all of the API functions are defined.


;;; flowrweb-api.clj ends here
