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

(defn push [target & facts]
  "Asserts one or more facts (ie a Jena triples) into the target, which can
   be either a model or remote endpoint uri."
  (push-impl target facts))

(defn resource-fact [subject predicate object]
  "Returns a Jena triple that can be added to a model, where the
   object is a string that is a valid uri and is intended to be 
   interpreted as a resource.  The subject and predicate strings 
   must be valid uris, except in the case where the subject is a string
   that starts with '_:', in which case it is interpreted as a blank node."
  (resource-fact-impl subject predicate object))

(defn literal-fact [subject predicate object]
  "Returns a Jena triple that can be added to a model, 
   where the object is a literal value.  Strings, integers,
   longs, doubles, booleans, and dates (instances of java.util.Date)
   will be converted to the appropriate XSD datatype.  Others will 
   either turn into strings or fail. The subject and predicate strings 
   must be valid uris, except in the case where the subject is a string
   that starts with '_:', in which case it is interpreted as a blank node."
  (literal-fact-impl subject predicate object))