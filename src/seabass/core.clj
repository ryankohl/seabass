(ns seabass.core
  (:use [seabass.impl]))
					
(defn build  [& targets]
  "Creates a model from the arguments provided.
  These arguments can include local uri and remote
  uri strings as well as rdf files and models returned from a 
  (pull query target) call."
(build-impl targets))

(defn bounce [query target]
  "Returns a map using a SELECT query.  If the 
  target is a URI, then a Sparql Endpoint is 
  interrogated.  If the target is a model, then the
  model is interrogated.
  The map has two keys -
    :vars - the list of variables used in the select query's
            'projection' clause (ie the variables you select)
    :data - a list of maps whose keys are the variables listed
            in vars"
(bounce-impl query target))
				
(defn ask [query target]
  "Returns a boolean using an ASK query.  If the 
  target is a URI, then a Sparql Endpoint is 
  interrogated.  If the target is a model, then the
  model is interrogated."
(ask-impl query target))

(defn pull [query target]
  "Returns a model using a CONSTRUCT query.  If the 
  target is a URI, then a Sparql Endpoint is 
  interrogated.  If the target is a model, then the
  model is interrogated."
(pull-impl query target))
	
(defn stash [model target]
  "Writes the contents of a model to the file specified
  by the target string.  The resulting file is written in
  n-triples.  Only RDF facts are written (i.e. not rules).
  Returns the path name of the written file (ie the
  provided 'target' parameter)."
(stash-impl model target))