(ns seabass.core
  (:import [org.apache.jena.rdf.model Model ModelFactory])
  (:import [org.apache.jena.query QueryFactory QueryExecutionFactory
	    ResultSet ResultSetFormatter])
  (:import [org.apache.jena.reasoner.rulesys GenericRuleReasonerFactory Rule])
  (:import [org.apache.jena.vocabulary ReasonerVocabulary])
  (:import [org.apache.jena.datatypes TypeMapper])
  (:import [org.apache.jena.datatypes.xsd XSDDateTime])
  (:import [org.apache.jena.reasoner.rulesys.builtins BaseBuiltin])
  (:import [org.apache.jena.graph NodeFactory BlankNodeId])
  (:import [org.apache.jena.sparql.modify.request QuadDataAcc UpdateDataInsert])
  (:import [org.apache.jena.update UpdateAction UpdateExecutionFactory])
  (:import [org.apache.jena.atlas.web.auth PreemptiveBasicAuthenticator ScopedAuthenticator])
  (:import [java.net URI])
  (:require [seabass.impl :refer :all]))

(defn build
  "Creates a model from the arguments provided.
  These arguments can include local uri and remote
  uri strings as well as rdf files and models returned from a
  (pull query target) call."
  ([& urls]
   (let [core (ModelFactory/createDefaultModel)
         config (.addProperty (.createResource core)
                              ReasonerVocabulary/PROPruleMode
                              "hybrid")
         reasoner (.create (GenericRuleReasonerFactory/theInstance) config) ]
     (registerBuiltins)
     (doseq [x urls]
       (cond
         (file? x) (add-file core x)
         (vector? x) (.add core (get-model (nth x 0) (nth x 1)))
         (model? x) (.add core x)
         (rules? x) (.setRules reasoner (Rule/rulesFromURL x))
         (string? x) (.add core (get-model x)) ))
     (ModelFactory/createInfModel reasoner core))))

(defn bounce
  "Returns a map using a SELECT query.  If the
  target is a URI, then a Sparql Endpoint is
  interrogated.  If the target is a model, then the
  model is interrogated.
  The map has two keys -
    :vars - the list of variables used in the select query's
            'projection' clause (ie the variables you select)
    :data - a list of maps whose keys are the variables listed
            in vars"
  ([query target]
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
             format-result-set)))
  ([query target username password]
   ([query target username password]
    (cond (string? target)
          (let [auth (PreemptiveBasicAuthenticator.
                      (ScopedAuthenticator. (URI. target)
                                            username
                                            (char-array password)))]
            (-> target
                (QueryExecutionFactory/sparqlService ,,,  (prefixes query) auth)
                .execSelect
                format-result-set))
          (model? target)
          "Basic auth only defined for remote models"))))

(defn ask
  "Returns a boolean using an ASK query.  If the
  target is a URI, then a Sparql Endpoint is
  interrogated.  If the target is a model, then the
  model is interrogated."
  [query target]
  (cond (string? target)
        (-> target
            (QueryExecutionFactory/sparqlService ,,, (prefixes query))
            .execAsk)
        (model? target)
        (-> (prefixes query)
            QueryFactory/create
            (QueryExecutionFactory/create ,,, target)
            .execAsk)))

(defn pull
  "Returns a model using a CONSTRUCT query.  If the
  target is a URI, then a Sparql Endpoint is
  interrogated.  If the target is a model, then the
  model is interrogated."
  [query target]
  (cond (string? target)
        (-> target
            (QueryExecutionFactory/sparqlService ,,, (prefixes query))
            .execConstruct)
        (model? target)
        (-> (prefixes query)
            QueryFactory/create
            (QueryExecutionFactory/create ,,, target)
            .execConstruct)))

(defn stash
  "Writes the contents of a model to the file specified
  by the target string.  The resulting file is written in
  n-triples.  Only RDF facts are written (i.e. not rules).
  Returns the path name of the written file (ie the
  provided 'target' parameter)."
  [model target]
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

(defn push
  "Asserts a collection of facts (ie of Jena triples) into the target, which can
   be either a model or remote endpoint uri."
  [target facts]
  (let [qda (QuadDataAcc.)]
    (doseq [t facts] (.addTriple qda t))
    (cond (model? target) (UpdateAction/execute
                           (UpdateDataInsert. qda)
                           target)
          (uri? target) (-> (UpdateExecutionFactory/createRemoteForm (UpdateDataInsert. qda) target)
                            .execute)
          :else (throw (Exception. (str "Target must be either a Jena model "
                                        "or a uri to a sparql endpoint"))))))

(defn resource-fact
  "Returns a Jena triple that can be added to a model, where the
   object is a string that is a valid uri and is intended to be
   interpreted as a resource.  The subject and predicate strings
   must be valid uris, except in the case where the subject is a string
   that starts with '_:', in which case it is interpreted as a blank node."
  [subject predicate object]
  (cond (uri? object) (make-triple subject predicate (NodeFactory/createURI object))
        (.startsWith object "_:") (make-triple subject predicate (NodeFactory/createBlankNode (BlankNodeId. object)))
        :else (throw (Exception. "Object must be a valid uri"))))

(defn literal-fact
  "Returns a Jena triple that can be added to a model,
   where the object is a literal value.  Strings, integers,
   longs, doubles, booleans, and dates (instances of java.util.Date)
   will be converted to the appropriate XSD datatype.  Others will
   either turn into strings or fail. The subject and predicate strings
   must be valid uris, except in the case where the subject is a string
   that starts with '_:', in which case it is interpreted as a blank node."
  [subject predicate object]
  (let [tm (TypeMapper/getInstance)]
    (cond
      (date? object) (let [cal (java.util.Calendar/getInstance)]
                       (.setTime cal object)
                       (make-triple subject predicate (make-literal (XSDDateTime. cal) tm)))
      :else (make-triple subject predicate (make-literal object tm)))))
