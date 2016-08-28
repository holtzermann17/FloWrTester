(ns flowrweb.core
  (:require [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.zip :as z]
            [me.raynes.conch.low-level :as sh]
            ;[pipes.core :refer [->pipe pipe-> ->pipe-> ->job->]]
            ;[pipes.shell :refer [exec]]
            [clj-wordnet.core :refer :all]
            [clojure.core.async :refer [chan close! go >! <!!]]
            [clojure.edn :as edn]
            [clojure-word2vec.core :refer :all]
            [clojure.java.io :as io]
            [incanter.core :as incanter]
            [incanter.stats :as stats])
  (:gen-class))

;; Make some convenient tools for printing available.
(use '[clojure.pprint :only (pprint)])

;; useful string processing functionality
(require '[clojure.string :as str])

;; preserve sanity in case of large printouts
;; ... what's the corresponding variable for depth?
(set! *print-length* 10) 
(set! *print-level* 10)

;; ... and this gives a work-around in case one needs to inspect the entire result
(defn print-all
  "Run BODY and then print the entire result."
  [body]
  (let [result (do body)]
    (binding [*print-length* nil
              *print-level* nil]
      (prn result))))

(defn pprint-all
  "Run BODY and then print the entire result."
  [body]
  (let [result (do body)]
    (binding [*print-length* nil
              *print-level* nil]
      (pprint result))))

(defn return-all
  "Run BODY and then print the entire result."
  [body]
  (let [result (do body)]
    (binding [*print-length* nil
              *print-level* nil]
      result)))

;;; Basic global variables (see flowrweb-client.clj for more interesting data structures)

;; I'm putting these here because the token is likely to change
;; frequently, but the rest of this file is NOT likely to change much,
;; so I can ignore it in most day-to-day commits.

(def flowrweb-url "http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/")
(def flowrweb-user "holtzermann17@gmail.com")
(def flowrweb-token "Kw1AF9iLZ4oigRnap83IvKSUAYtNOG14")

;;; Bootstrapping steps as needed

(def timestamp-formatter (tf/formatter "yyyy-MM-dd hh:mm:ss"))

;; Setting up this parsing stuff takes a few seconds
(load "nlp-tools")
;; Functions for extracting triples from text
(load "extractor")

;; Incorporating some code from https://github.com/Bridgei2i/clojure-word2vec
;; Building word2vec models
;
; (def ulysses-data (create-input-format "/home/joe/FloWrTester/flowrweb/src/clojure-word2vec/resources/ulysses.txt"))
; (def ulysses-model (word2vec ulysses-data))
;
;; Hm... Here are some pre-trained ones: https://github.com/3Top/word2vec-api#where-to-get-a-pretrained-models

;;; Utility functions
(load "utility")

;;; Load additional files containing relevant functionality:

;; Hook into the FloWr API:
(load "flowrweb-api")

;; Functions for making use of the information in the FloWr API
(load "flowrweb-client")

;; Load some functions for working with a local mock-up of automatic programming in FloWr using Clojure functions
(load "fake-flowrs-tests")
(load "fake-flowrs-meta")
(load "fake-flowrs")

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "I'm a little teapot!")
  ;; can set :escape-non-ascii to true and get \u00A3 instead of £
  (println (generate-string {:foo "It costs £100"} {:escape-non-ascii false}))
  (println "The time is now:" (tf/unparse timestamp-formatter (t/now)))
  ;; NLP stuff
  (println (:gloss (first (wordnet "teapot" :noun))))
  (println (first (treebank-parser ["I'm a little teapot !"])))
  (pprint (triplet-extraction
           (first (treebank-parser
                   ["A rare black squirrel has become a regular visitor to a suburban garden ."]))))
  ;; remove this for now
  ;;
  ;; (println (get-matches ulysses-model "woman"))
  ;; Generic testing
;  (println (test-access))
;  (println (list-all-nodes))
;  (println (list-user-charts))
)
