(ns flowrweb.core
  (:require [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as tf])
  (:gen-class))

;; Make some convenient tools for printing available.
(use '[clojure.pprint :only (pprint)])

;; useful string processing functionality
(require '[clojure.string :as str])

;;; Basic global variables (see flowrweb-client.clj for more interesting data structures)

;; I'm putting these here because the token is likely to change
;; frequently, but the rest of this file is NOT likely to change much,
;; so I can ignore it in most day-to-day commits.

(def flowrweb-url "http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/")
(def flowrweb-user "holtzermann17@gmail.com")
(def flowrweb-token "QVq0eogdVhUeOF8jGfkwBfZCHSHCTOzq")

;;; Bootstrapping steps as needed

(def timestamp-formatter (tf/formatter "yyyy-MM-dd hh:mm:ss"))

;;; Utility functions

(load "utility")

;;; Load additional files containing relevant functionality:

;; Hook into the API:
(load "flowrweb-api")

;; Functions for making use of the information in the API
(load "flowrweb-client")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "I'm a little teapot!")
  (println (generate-string {:foo "It costs £100"} {:escape-non-ascii true}))
  (println "The time is now:" (tf/unparse timestamp-formatter (t/now)))
;  (println (test-access))
;  (println (list-all-nodes))
;  (println (list-user-charts))
)
