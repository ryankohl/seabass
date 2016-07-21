(ns seabass.impl
  (:import [org.apache.jena.rdf.model ModelFactory])
  (:import [org.apache.jena.util FileUtils])
  (:import [org.apache.jena.reasoner.rulesys BuiltinRegistry])
  (:import [org.apache.jena.graph NodeFactory Triple BlankNodeId])
  (:require [seabass.builtin :as builtin]
            [clojure.string :as str]))

(defn date? [x] (= (type x) java.util.Date))
(defn rules? [x]  (= (last (str/split x #"\.")) "rules"))
(defn uri?   [x]  (FileUtils/isURI x))
(defn file? [x] (= (.getClass x) java.io.File))
(defn model? [x]
  (let [m "class org.apache.jena.rdf.model.impl.ModelCom"
        i "class org.apache.jena.rdf.model.impl.InfModelImpl"
	klass (str (class x))	]
    (or (= klass m) (= klass i)) ))

(defn get-model
  ( [] (ModelFactory/createDefaultModel))
  ( [filename]
   (get-model filename (FileUtils/guessLang filename)))
  ( [filename lang]
   (let [model (get-model) ]
     (try (let [url (java.net.URL. filename)]
            (.read model filename lang))
          (catch java.net.MalformedURLException e
            (.read model (java.io.FileInputStream. filename) "" lang))))))

(defn add-file [model file]
  (let [filename (.getName file)
        lang (FileUtils/guessLang filename)]
    (.read model (java.io.FileInputStream. file) "" lang)))

(defn registerBuiltins []
  (.register BuiltinRegistry/theRegistry builtin/diff-second)
  (.register BuiltinRegistry/theRegistry builtin/diff-minute)
  (.register BuiltinRegistry/theRegistry builtin/diff-hour)
  (.register BuiltinRegistry/theRegistry builtin/diff-day) )

(defn get-value [node]
  (cond
    (nil? node) nil
    (.isLiteral node) (.getValue node)
    (.isResource node) (.toString node)))

(defn get-solution [cols result]
  (zipmap (map keyword cols)
          (map #(get-value (.get result %)) cols)))

(defn get-solutions [cols result-set]
  (loop [soln []]
    (if-not (.hasNext result-set)
      soln
      (recur (cons (get-solution cols (.next result-set)) soln)))))


(defn format-result-set [result-set]
  (let [cols (seq (.getResultVars result-set))
        data (get-solutions cols result-set)]
    {:vars (map keyword cols),
     :data data}))

(defn prefixes [query]
  (let [p "
prefix xsd:     <http://www.w3.org/2001/XMLSchema#>
prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
prefix owl:     <http://www.w3.org/2002/07/owl#>  \n"]
    (str p query)))

(defn make-triple [s p o]
  (cond (.startsWith s "_:") (cond (uri? p) (Triple/create
                                             (NodeFactory/createBlankNode (BlankNodeId. s))
                                             (NodeFactory/createURI p)
                                             o)
                                   :else (throw
                                          (Exception. "Predicate must be a valid uri")))

        :else  (cond (not-every? uri? [s p]) (throw
                                              (Exception. "Every term must be a valid url"))
                     :else (Triple/create
                            (NodeFactory/createURI s)
                            (NodeFactory/createURI p)
                            o))))

(defn make-literal [x type-mapper]
  (NodeFactory/createLiteral
   (str x)
   (.getTypeByValue type-mapper x)))
