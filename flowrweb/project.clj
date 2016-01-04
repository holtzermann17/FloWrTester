(defproject flowrweb "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for FloWrWeb"
  :url "http://metameso.org/~joe"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.11.0"]]
  :main ^:skip-aot flowrweb.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

;;; Requirements are:

;; https://github.com/dakrone/cheshire
;; https://github.com/dakrone/clj-http
;; https://github.com/clj-time/clj-time
