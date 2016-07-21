(defproject seabass "2.1.2"
  :description "A library for working with RDF."
  :url "https://github.com/ryankohl/seabass"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.apache.jena/apache-jena-libs "3.1.0" :extension "pom"]
                 [xerces/xercesImpl "2.11.0"]
                 ]
  :plugins [[lein-clojars "0.9.1"]]
  )
