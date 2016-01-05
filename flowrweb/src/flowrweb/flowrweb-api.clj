;;; Commentary:

;; Define Clojure functions that communicate with FloWr over the API.
;; Relies on global variables defined in core.clj.

;;; Code:

;; we will create a macro that creates all of the API
;; functions more or less in one fell swoop.  It will take:
;; - function name
;; - doc string
;; - list of arguments
;; - an indication of whether or not to decode JSON.

;; We build this macro up step by step.

(defn convert-map-to-url-string
  "Transform a map like {:a \"foo\", :b \"bar\"} to a string like \"a=foo&b=bar\"."
  [m]
  (str/join "&"
        (map (fn [[k v]] 
               (str (name k) "=" v))
             m)))

(defn post 
  "Send COMMAND to the FloWr Web URL, and decide whether or not to DECODE-JSON depending on the value of that variable.
\"Optional\" arguments ARGS are those required by the COMMAND and should be fed in as a" 
  [command decode-json
   & {:keys [args]}]
  (let [data {:api_token flowrweb-token
              :api_email flowrweb-user
              :c command}
        resp (client/post flowrweb-url 
                          ;; Note, the FloWr web API does not currently accept JSON
                          ;; but rather a URL-parameter-like body.
                          {:body (convert-map-to-url-string (merge data args))
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

;;; Testing code, superseded by `create-api-function' and calls to the same, below.

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

;; We can use this macro to define the functions we need LISP-style names
(defmacro create-api-function [fnname decode-json & arguments]
  `(do (defn ~(symbol (name fnname))
         [~@arguments]
         (post ~(str/replace (name fnname) "-" "_")
               ~decode-json
               :args ~(convert-seq-to-map arguments)))))

;; Aside: you can't `apply' macros in Clojure, though there are probably some clever
;; "Let Over Lambda" style ways to make it work, at least in limited cases.
;; See `apply-macro' and `apply-macro-1' in core.clj for some attempts.

;; For now, the following doesn't take up too much space and gets the job done:

(create-api-function test-access             false)
(create-api-function list-all-nodes          true)
(create-api-function list-user-charts        true)
(create-api-function new-chart               false)
(create-api-function delete-chart            false cid)
(create-api-function add-node                false cid type)
(create-api-function delete-node             true  cid nid)
(create-api-function get-chart               true  cid)
(create-api-function clear-output            false cid)
(create-api-function run-chart               false cid)
(create-api-function run-status              true  cid)
(create-api-function get-parameters          true  cid nid)
(create-api-function set-parameter           false cid nid pname pvalue)
(create-api-function new-variable            false cid nid)
(create-api-function rename-variable         true  cid nid vname nname)
(create-api-function delete-variable         true  cid nid vname)
(create-api-function get-variables           true  cid nid)
(create-api-function get-output-tree         true  cid nid)
(create-api-function set-variable-definition true  cid nid vname vdef)
(create-api-function get-node-output         true  cid nid)
(create-api-function get-variable-output     true  cid vname)
(create-api-function node-type-info          true  type)

;; And with that, all of the API functions are defined.

;;; flowrweb-api.clj ends here
