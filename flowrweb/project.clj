(defproject flowrweb "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for FloWrWeb"
  :url "http://metameso.org/~joe"
  :jvm-opts ["-Xmx3g"]
  :dependencies [[org.clojure/clojure "1.8.0"] ;; note: to play with the latest clojure.spec stuff, bump this up...
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [instaparse "1.4.1"]
                 [me.raynes/conch "0.8.0"]
                 ;[bilus/pipes "0.1.2"]
                 [clojure-opennlp "0.3.3"]
                 [clj-wordnet "0.1.0"]
                 [org.bridgei2i/word2vec "0.2.1"]
                 [clj-time "0.11.0" :exclusions [org.clojure/clojure]]
                 [org.clojure/core.async "0.2.374"]]
  :main ^:skip-aot flowrweb.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

;;; Requirements are:

;; https://github.com/dakrone/cheshire
;; https://github.com/dakrone/clj-http
;; https://github.com/clj-time/clj-time
;; https://github.com/dakrone/clojure-opennlp
;; https://github.com/delver/clj-wordnet

;; Some of them ask for lots of memory, hence the :jvm-opts instruction
