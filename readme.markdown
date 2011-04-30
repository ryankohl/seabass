seabass
----

This is a small library to make it easier to work with RDF and SPARQL when using Incanter.

API
----
There are four functions defined in this library:

build [ & targets ]
====

Takes n-many arguments and returns an RDF model.  These arguments can be
	
-   Strings for relative or absolute pathnames for RDF files
-   Strings for URI's that resolve to an RDF file online
-   Vectors for RDF files where the first element is the URI/pathname, and the 
  second element is the language (when the file suffix isn't sufficient).
  Valid language strings (following Jena) are:
	-   "RDF/XML"
	-   "N-TRIPLES"
	-   "TTL"
	-   "N3"
-   Strings for Jena Rules files (must end with ".rules" in the filename...for now)
-   RDF models previously created with the build function
-   I suppose you could use a Jena model, since that's the underlying implementation
		
bounce [ query target ]
====

This execute a SELECT query against an RDF model, returning an Incanter 
Dataset.  The arguments are:
	
-   query: a SPARQL Select query string
-   target: either a URI string for a Sparql Endpoint or an RDF model
		
ask [ query target ]
====

This executes an ASK query against an RDF model, returning a Boolean 
value.  The arguments are:

-   query: a SPARQL Ask query string
-   target: either a URI string for a Sparql Endpoint or an RDF model
		
pull [ query target ]
====

This executes a CONSTRUCT query against an RDF model, returning a 
new RDF model.  The arguments are:

- 	query: a SPARQL Construct query string
- 	target: either a URI string for a Sparql Endpoint or an RDF model

Usage
----

-   Build an RDF model with a local TTL file, a remote NT file, and a model pulled from an Endpoint

```clj
(def c "	construct ?x <http://seabass.foo/bar> ?y 
			where { ?y <http://example.org/baz> ?x }")
(build    ["data/my-ontology.rdf" "TTL"] 
			"http://way.out.there/my-data.nt" 
			(pull c "http://my-endpoint/sparql"))
```
	
-   Ask whether a Sparql endpoint is up

```clj
(ask "ask {}" "http://my-endpoint/sparql")
```
	
-   Create an Incanter Dataset based on a Select query

```clj
(def q "	select ?x ?y ?z 
			where {	
				?x <http://ex.org/foo> ?y . 
				?z <http://ex.org/bar ?y . }")
(bounce q (build "data/my-ont.ttl" "data/your-ont.owl"))
```

Built-ins
----
All built-ins supported in Jena 2.6.4 (the current release as of April, 2011) can be used.  The following built-ins have been added for Jena Rules (i.e. in .rules files, and not in Sparql queries):

-  diff-second: Returns the difference between two times, dates, or datetimes in seconds.
-  diff-minute: Returns the difference between two times, dates, or datetimes in minutes.
-  diff-hour: Returns the difference between two times, dates, or datetimes in hours.
-  diff-day: Returns the difference between two times, dates, or datetimes in days (1 day = 24 hours).

License
----

Copyright (C) 2011 Ryan Kohl

Distributed under the Eclipse Public License, the same as Clojure.
