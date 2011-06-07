(ns seabass.impl
  (:import [com.hp.hpl.jena.rdf.model Model ModelFactory])
  (:import [com.hp.hpl.jena.query QueryFactory QueryExecutionFactory
	    ResultSet ResultSetFormatter])
  (:import [com.hp.hpl.jena.reasoner.rulesys GenericRuleReasonerFactory Rule])
  (:import [com.hp.hpl.jena.vocabulary ReasonerVocabulary])
  (:import [com.hp.hpl.jena.util FileUtils])
  (:import [com.hp.hpl.jena.sparql.engine.http QueryExceptionHTTP])
  (:import [javax.xml.bind DatatypeConverter])
  (:import [com.hp.hpl.jena.reasoner.rulesys.builtins BaseBuiltin])
  (:import [com.hp.hpl.jena.reasoner.rulesys BuiltinRegistry Util])
  (:import [com.hp.hpl.jena.graph Node])
  (:require [clojure.contrib [math :as math] [string :as str] [json :as json] ]
	    [incanter.core :as incanter]
	    [seabass.builtin :as builtin]))	
					
(defn rules? [x]  (= (str/trim (str/tail 6 x)) ".rules"))
(defn file?  [x]  (FileUtils/isFile x))
(defn uri?   [x]  (FileUtils/isURI x))

(defn model? [x]
  (let [m "class com.hp.hpl.jena.rdf.model.impl.ModelCom" 
        i "class com.hp.hpl.jena.rdf.model.impl.InfModelImpl" 
	klass (str (class x))	]
    (or (= klass m) (= klass i)) ))
		
(defn save-model-impl [model target]
  (with-open [ stream 	(java.io.FileOutputStream. target)]
    (let [m (.add (ModelFactory/createDefaultModel) model)
	  p (.getProperty model
			  "http://jena.hpl.hp.com/2003/RuleReasoner#"
			  "ruleMode" ) ]
      (.write (.removeAll m nil p nil) stream "N-TRIPLE")
      target )))
		
(defn get-model  
  ( [] (ModelFactory/createDefaultModel))
  ( [url-filename]
      (get-model url-filename (FileUtils/guessLang url-filename)))
  ( [url-filename lang]
      (let [model (get-model) ]
	(try (let [url (java.net.URL. url-filename)]
	       (.read model url-filename lang))
	     (catch java.net.MalformedURLException e
	       (.read model (java.io.FileInputStream. url-filename) "" lang))))))
 			
(defn registerBuiltins []
  (.register BuiltinRegistry/theRegistry builtin/diff-second)
  (.register BuiltinRegistry/theRegistry builtin/diff-minute)
  (.register BuiltinRegistry/theRegistry builtin/diff-hour)
  (.register BuiltinRegistry/theRegistry builtin/diff-day) )

(defn build-impl  
  [urls]
  (let [m "class com.hp.hpl.jena.rdf.model.impl.ModelCom"
	i "class com.hp.hpl.jena.rdf.model.impl.InfModelImpl"
	core (ModelFactory/createDefaultModel)
	config (.addProperty (.createResource core)
			     ReasonerVocabulary/PROPruleMode "hybrid")
	reasoner (.create (GenericRuleReasonerFactory/theInstance) config) ]
    (try (registerBuiltins)
	 (doseq [x urls]
	   (cond
	    (vector? x) (.add core (get-model (nth x 0) (nth x 1)))
	    (model? x) (.add core x)
	    (rules? x) (.setRules reasoner (Rule/rulesFromURL x))
	    (string? x) (.add core (get-model x)) ))
	 (ModelFactory/createInfModel reasoner core)
	 (catch Exception e (prn e)))))

(defn getValue [key map]
  (try 
    (let [ jsonVal (get-in map [key :value]) ]
      (condp = (get-in map [key :datatype])  
	"http://www.w3.org/2001/XMLSchema#boolean"
	(str (DatatypeConverter/parseBoolean jsonVal))
	
	"http://www.w3.org/2001/XMLSchema#date"
	(.getTimeInMillis (DatatypeConverter/parseDate jsonVal))
	
	"http://www.w3.org/2001/XMLSchema#dateTime"
	(.getTimeInMillis (DatatypeConverter/parseDateTime jsonVal))
	
	"http://www.w3.org/2001/XMLSchema#decimal"
	(.doubleValue (DatatypeConverter/parseDecimal jsonVal))
	
	"http://www.w3.org/2001/XMLSchema#double"
	(Double/parseDouble jsonVal)
	
	"http://www.w3.org/2001/XMLSchema#float"
	(DatatypeConverter/parseFloat jsonVal)
	
	"http://www.w3.org/2001/XMLSchema#integer"
	(DatatypeConverter/parseInt jsonVal)
	
	"http://www.w3.org/2001/XMLSchema#string"
	(DatatypeConverter/parseString jsonVal)
	
	"http://www.w3.org/2001/XMLSchema#time"
	(.getTimeInMillis (DatatypeConverter/parseTime jsonVal))

	jsonVal))
    (catch IllegalArgumentException e (prn e))))

(defn shallow
  "take the default decoding of a json'd result set
   and turn it into a more incanter-like vector"
  [result] (zipmap (keys result) (map #(getValue % result) (keys result))))
	
(defn format-result-set [result-set]
  (with-open [ stream (java.io.ByteArrayOutputStream.)]
    (ResultSetFormatter/outputAsJSON stream result-set)
    (let [decoded (json/read-json (.toString stream))
	  results (:bindings (:results decoded)) 
	  cols (vec (keys (first results)))
	  data (vec (map shallow results))]
      (conj [cols] data))))

(defn makeDataset [results] (incanter/dataset (get results 0) (get results 1)))

(defn prefixes [query]
  (let [p "
prefix xsd:     <http://www.w3.org/2001/XMLSchema#> 
prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
prefix rdfs:     <http://www.w3.org/2000/01/rdf-schema#> 
prefix owl:     <http://www.w3.org/2002/07/owl#>  \n"]
    (str p query)))

(defn execute-select [query target]
  (try 
    (cond (string? target)
	  (-> target
	      (QueryExecutionFactory/sparqlService ,,,  (prefixes query))
	      .execSelect
	      format-result-set
	      makeDataset)
	  (model? target)
	  (-> (prefixes query)
	      QueryFactory/create
	      (QueryExecutionFactory/create ,,, target)
	      .execSelect
	      format-result-set
	      makeDataset))
    (catch QueryExceptionHTTP e (prn e))))

(defn execute-ask 
  [query target]
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
    (catch QueryExceptionHTTP e (prn e))))
		
(defn execute-construct [query target]
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
    (catch QueryExceptionHTTP e (prn e))))