(ns seabass.impl
  (:import [com.hp.hpl.jena.rdf.model Model ModelFactory])
  (:import [com.hp.hpl.jena.query QueryFactory QueryExecutionFactory
	    ResultSet ResultSetFormatter])
  (:import [com.hp.hpl.jena.reasoner.rulesys GenericRuleReasonerFactory Rule])
  (:import [com.hp.hpl.jena.vocabulary ReasonerVocabulary])
  (:import [com.hp.hpl.jena.util FileUtils])
  (:import [com.hp.hpl.jena.datatypes TypeMapper])
  (:import [com.hp.hpl.jena.datatypes.xsd XSDDateTime])
  (:import [com.hp.hpl.jena.reasoner.rulesys.builtins BaseBuiltin])
  (:import [com.hp.hpl.jena.reasoner.rulesys BuiltinRegistry Util])
  (:import [com.hp.hpl.jena.graph NodeFactory Triple])
  (:use [clojure.java.io])
  (:require [seabass.builtin :as builtin]
            [clojure.string :as str]))

(defn date? [x] (= (type x) java.util.Date))					
(defn rules? [x]  (= (last (str/split x #"\.")) "rules"))
(defn uri?   [x]  (FileUtils/isURI x))
(defn file? [x] (= (.getClass x) java.io.File))
(defn model? [x]
  (let [m "class com.hp.hpl.jena.rdf.model.impl.ModelCom" 
        i "class com.hp.hpl.jena.rdf.model.impl.InfModelImpl" 
	klass (str (class x))	]
    (or (= klass m) (= klass i)) ))

(defn stash-impl [model target]
  (with-open [ stream (java.io.FileOutputStream. target)]
    (let [p (.getProperty model
			  "http://jena.hpl.hp.com/2003/RuleReasoner#"
                          "ruleMode")]
      (.removeAll model nil p nil)
      (.write model stream "N-TRIPLE")
      (.addProperty (.createResource model)
                    ReasonerVocabulary/PROPruleMode
                    "hybrid")
      target )))

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

(defn build-impl [urls]
  (let [core (ModelFactory/createDefaultModel)
	config (.addProperty (.createResource core)
			     ReasonerVocabulary/PROPruleMode
                             "hybrid")
	reasoner (.create (GenericRuleReasonerFactory/theInstance) config) ]
    (try (registerBuiltins)
	 (doseq [x urls]
	   (cond
            (file? x) (add-file core x)
	    (vector? x) (.add core (get-model (nth x 0) (nth x 1)))
	    (model? x) (.add core x)
	    (rules? x) (.setRules reasoner (Rule/rulesFromURL x))
	    (string? x) (.add core (get-model x)) ))
	 (ModelFactory/createInfModel reasoner core)
	 (catch Exception e (prn e)))))

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

(defn bounce-impl [query target]
  (try 
    (cond (string? target)
	  (-> target
	      (QueryExecutionFactory/sparqlService ,,,  (prefixes query))
	      .execSelect
	      format-result-set)
	  (model? target)
	  (-> (prefixes query)
	      QueryFactory/create
	      (QueryExecutionFactory/create ,,, target)
	      .execSelect
	      format-result-set))
    (catch Exception e (prn e))))

(defn ask-impl [query target]
  (try
    (cond (string? target)
	  (-> target
	      (QueryExecutionFactory/sparqlService ,,, (prefixes query))
	      .execAsk)
	  (model? target)
	  (-> (prefixes query)
	      QueryFactory/create
	      (QueryExecutionFactory/create ,,, target)
	      .execAsk))
    (catch Exception e (prn e))))

(defn pull-impl [query target]
  (try 
    (cond (string? target)
	  (-> target
	      (QueryExecutionFactory/sparqlService ,,, (prefixes query))
	      .execConstruct)
	  (model? target)
	  (-> (prefixes query)
	      QueryFactory/create
	      (QueryExecutionFactory/create ,,, target)
	      .execConstruct))
    (catch Exception e (prn e))))

(defn make-triple [s p o]
  (cond (.startsWith s "_:") (cond (uri? p) (Triple/create
                                             (NodeFactory/createAnon s)
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

(defn resource-fact-impl [s p o]
  (cond (uri? o) (make-triple s p (NodeFactory/createURI o))                 
        :else (throw (Exception. "Object must be a valid uri"))))


(defn literal-fact-impl [s p o]
  (let [tm (TypeMapper/getInstance)]
    (cond 
     (date? o) (let [cal (java.util.Calendar/getInstance)]
                 (.setTime cal o)
                 (make-triple s p (make-literal (XSDDateTime. cal) tm)))
     :else (make-triple s p (make-literal o tm)))))


