seabass
----

This is a small library to make it easier to work with RDF and SPARQL in Clojure 1.4.  

Default Prefixes
----

- xsd  (http://www.w3.org/2001/XMLSchema#)
- rdf  (http://www.w3.org/1999/02/22-rdf-syntax-ns#)
- rdfs (http://www.w3.org/2000/01/rdf-schema#)
- owl  (http://www.w3.org/2002/07/owl#)

API
----

build [ & targets ]
====

Takes n-many arguments and returns an RDF model.  These arguments can be
	
- Strings for relative or absolute pathnames for RDF files
- Strings for URI's that resolve to an RDF file online
- Vectors for RDF files where the first element is the URI/pathname, and the 
  second element is the language (when the file suffix isn't sufficient).
  Valid language strings (following Jena) are:
	- "RDF/XML"
	- "N-TRIPLES"
	- "TTL"
	- "N3"
- Strings for Jena Rules files (must end with ".rules" in the filename)
- RDF models previously created with the build function
- A Jena model
		
bounce [ query target ]
====

This executes a SELECT query against an RDF model, returning a map with 
the following keys:
- :vars - the list of variables used in the select query's 'projection' clause
- :data - a list of maps whose keys are the variables listed in :vars

The arguments are:
	
- query: a SPARQL Select query string
- target: either a URI string for a Sparql Endpoint or an RDF model
		
ask [ query target ]
====

This executes an ASK query against an RDF model, returning a Boolean 
value.  The arguments are:

- query: a SPARQL Ask query string
- target: either a URI string for a Sparql Endpoint or an RDF model
		
pull [ query target ]
====

This executes a CONSTRUCT query against an RDF model, returning a 
new RDF model.  The arguments are:

- query: a SPARQL Construct query string
- target: either a URI string for a Sparql Endpoint or an RDF model

stash [ model target ]
====

Writes the contents of a model to the file specified by the target string.  
The resulting file is encoded in n-triples.  Only RDF facts are written 
(i.e. not rules).  Returns the path name of the written file (ie the 
provided 'target' parameter).

- model: an RDF model previously constructed (via build or pull)
- target: a string for a relative or absolute pathname for the file to write to


resource-fact, literal-fact [subject predicate object]
====

These functions return Jena triples, which can be pushed into a model.

- subject: A string that is a valid uri or a string starting with '_:', indicating a blank node
- predicate: A string that is a valid uri
- object: 
-- resource-fact: A string that is a valid uri, or a string starting with '_:', indicating a blank node.  Will be interpreted as an RDF resource.
-- literal-fact: Either a string, integer, long, double, boolean, or date (java.util.Date).  The value will be converted to an appropriate RDF datatype.  If the conversion fails, either an exception will be thrown or a nil will be returned (depending on how crazy the submitted value is).

Usage
----

-   Build an RDF model with a local TTL file, a remote NT file, and a model pulled from an Endpoint

```clj
(def c "construct {?x <http://seabass.foo/bar> ?y}
	{ ?y <http://example.org/baz> ?x }")
(def m (build ["data/my-ontology.rdf" "TTL"] 
       "http://way.out.there/my-data.nt" 
       (pull c "http://my-endpoint/sparql")))
(def r1 (resource-fact "http://foo/luke" "http://foo/sibling" "http://foo/leia"))
(def r2 (resource-fact "_:v" "http://foo/father" "http://foo/luke"))
(def l1 (literal-fact "_:v" "http://foo/wears" "http://foo/cape"))
(push m r1 r2 l1)
```
	
-   Ask whether a Sparql endpoint is up

```clj
(ask "ask {}" "http://my-endpoint/sparql")
```
	
-   Execute a Select query

```clj
(def q "select ?x ?y ?z 
        {?x <http://ex.org/foo> ?y . 
         ?z <http://ex.org/bar ?y . }")
(def m  (build "data/my-ont.ttl" "data/your-ont.owl"))
(bounce q m)
```



Built-ins
----
All built-ins supported in Jena 2.9.2 (the current release as of April, 2011) can be used.  The following built-ins have been added for Jena Rules (i.e. in .rules files, and not in Sparql queries):

-  diff-second: Returns the difference between two times, dates, or datetimes in seconds.
-  diff-minute: Returns the difference between two times, dates, or datetimes in minutes.
-  diff-hour: Returns the difference between two times, dates, or datetimes in hours.
-  diff-day: Returns the difference between two times, dates, or datetimes in days (1 day = 24 hours).

License
----

Copyright (C) 2012 Ryan Kohl

Distributed under the Eclipse Public License, the same as Clojure.
