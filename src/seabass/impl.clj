(ns seabass.impl
	(:import [com.hp.hpl.jena.rdf.model Model ModelFactory])
	(:import [com.hp.hpl.jena.query QueryFactory QueryExecutionFactory ResultSet ResultSetFormatter])
	(:import [com.hp.hpl.jena.reasoner.rulesys GenericRuleReasonerFactory Rule])
	(:import [com.hp.hpl.jena.vocabulary ReasonerVocabulary])
	(:import [com.hp.hpl.jena.util FileUtils])
	(:import [com.hp.hpl.jena.sparql.engine.http QueryExceptionHTTP])
	(:require 	[clojure.contrib [string :as str] [json :as json] ]
					[incanter.core :as incanter]	))	

(defn rules?	[x]	(= (str/trim (str/tail 6 x)) ".rules"))
(defn file?	[x]	(FileUtils/isFile x))
(defn uri?	[x]	(FileUtils/isURI x))
 
(defn make-triple-from-json [subj pred obj tns]
	(str "_:b" subj " <" tns (str/as-str pred) "> " obj " .")	)

(defn model?	[x]
	(let [ m "class com.hp.hpl.jena.rdf.model.impl.ModelCom" 
			i	"class com.hp.hpl.jena.rdf.model.impl.InfModelImpl" 
			klass (str (class x))	]
		(or (= klass m) (= klass i))	))
		
(defn get-model  
	( []
		(ModelFactory/createDefaultModel)	)
	( [url-filename]
	(let [		model (get-model)	]
		(try (let [url (java.net.URL. url-filename)]
				(.read model url-filename (FileUtils/guessLang url-filename)))
		(catch java.net.MalformedURLException e
				(.read model (java.io.FileInputStream. url-filename) "" (FileUtils/guessLang url-filename)) )))))
				
(defn build-impl  [urls]
	(let [		m				"class com.hp.hpl.jena.rdf.model.impl.ModelCom"
				i				"class com.hp.hpl.jena.rdf.model.impl.InfModelImpl"
				core			(ModelFactory/createDefaultModel)
				config 		(.addProperty (.createResource core) ReasonerVocabulary/PROPruleMode "hybrid")
				reasoner	(.create (GenericRuleReasonerFactory/theInstance) config) ]
		(try	(doseq [x urls]	(cond
											(model? x)		(.add core x)
											(rules? x)		(.setRules reasoner (Rule/rulesFromURL x))
											(string? x)		(.add core (get-model x))	))
				(ModelFactory/createInfModel reasoner core)
				(catch Exception e nil)	)))
	
(defn getValue [key map]
	(let [ jsonVal (get-in map [key :value]) ]
		(condp = (get-in map [key :datatype])
			"http://www.w3.org/2001/XMLSchema#integer" (Integer/parseInt jsonVal)
			"http://www.w3.org/2001/XMLSchema#string" jsonVal
			jsonVal		)))
	
(defn shallow
	"take the default decoding of a json'd result set 
	and turn it into a more incanter-like vector"
	[result]	(zipmap (keys result) (map #(getValue % result) (keys result))) )
	
(defn format-result-set [result-set]
	(with-open [ stream 	(java.io.ByteArrayOutputStream.)]
		(ResultSetFormatter/outputAsJSON stream result-set)
		(let	[	decoded (json/read-json (.toString stream))
					results (:bindings (:results decoded)) 
					cols (vec (keys (first results)))
					data (vec (map shallow results))	]
				(conj [cols] data) 	)))

(defn makeDataset [results] 	(incanter/dataset (get results 0) (get results 1))	)

(defn execute-select [query target]    
	(try 
		(cond	(string? target)		
						(makeDataset (format-result-set (.execSelect (QueryExecutionFactory/sparqlService target query))))
					(model? target)	
						(makeDataset (format-result-set (.execSelect (QueryExecutionFactory/create (QueryFactory/create query) target))))	)
		(catch QueryExceptionHTTP e nil)	))

(defn execute-ask 
	[query target]
	(try
		(cond	(string? target) 		(.execAsk (QueryExecutionFactory/sparqlService target query))		
					(model? target)		(.execAsk (QueryExecutionFactory/create (QueryFactory/create query) target)) )
		(catch QueryExceptionHTTP e nil)	))
		
(defn execute-construct [query target]
	(try 
		(cond	(string? target)		(.execConstruct (QueryExecutionFactory/sparqlService target query))
					(model? target)		(.execConstruct (QueryExecutionFactory/create (QueryFactory/create query) target)) )
		(catch QueryExceptionHTTP e nil)	))