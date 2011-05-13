(ns seabass.test.core
	(:import [com.hp.hpl.jena.rdf.model Model])
	(:use [seabass.core] :reload)
	(:use [clojure.test])
	(:require 	[incanter.core :as incanter]	))
  
(deftest local-test
	(let [ m  (build "data/test.ttl" ["data/test.nt" "N-TRIPLES"]) 
			s1 "select distinct ?p where { ?s ?p ?o }"
			s2 "prefix sb: <http://seabass.foo/> select ?x ?y where { ?x sb:neighbor ?y }"
			a1 "prefix sb: <http://seabass.foo/> ask {sb:olivia sb:caught sb:carl}"
			a2 "prefix sb: <http://seabass.foo/> ask {sb:carl sb:caught sb:olivia}"
			c1 "prefix sb: <http://seabass.foo/> construct { ?x sb:neighbor ?y } { ?p sb:caught ?x . ?p sb:caught ?y . filter( ?x != ?y )} "
			]
		(is (= 18 (incanter/nrow (bounce s1 m))))
		(is (ask a1 m))
		(is (not (ask a2 m)))
		(is (= 0 (incanter/nrow(bounce s2 m))))
		(is (= 2 (incanter/nrow (bounce s2 (pull c1 m)))))
		(is (= 19 (incanter/nrow (bounce s1 (build m (pull c1 m))))))	))
		
(deftest add-test
	(let [ m 	(build "data/test.ttl" (build "data/test.nt"))
			 n 	(pile (build "data/test.ttl") (build "data/test.nt")) ]
			(is (.isIsomorphicWith m n))	))
		
(deftest remote-test
	(let [	s1 "select ?x where { ?x a <http://seabass.foo/Fish>  } limit 10"
			s2 "select ?p where { ?s ?p ?o }"
			c1 "construct {?x a <http://seabass.foo/Fish>} where { ?x a <http://www4.wiwiss.fu-berlin.de/factbook/ns#Country>}"
			endpoint "http://www4.wiwiss.fu-berlin.de/factbook/sparql"
			remote-xml "http://id.southampton.ac.uk/dataset/apps/latest.rdf"
			remote-ttl "http://id.southampton.ac.uk/dataset/apps/latest.ttl"
			m (build "data/test.ttl" "data/test.nt")	]
		(is (= 3 (incanter/nrow (bounce s1 m))))
		(is (= 10 (incanter/nrow (bounce s1 (build m (pull c1 endpoint))))))
		(is (< 0 (incanter/nrow (bounce s2 (build [remote-xml "RDF/XML"])))))
		(is (< 0 (incanter/nrow (bounce s2 (build [remote-ttl "TTL"])))))	))
		
(deftest reasoning-test
	(let [ m  (build "data/test.ttl" "data/test.nt") 
			s1 "prefix sb: <http://seabass.foo/> select ?x ?y where { ?x sb:neighbor ?y }"
			r	"data/test.rules" ]
		(is (= 0 (incanter/nrow (bounce s1 m))))
		(is (= 2 (incanter/nrow (bounce s1 (build m r)))))	))
			
(deftest datatype-test
	(let [ m (build "data/test.nt")
			s1 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:booleans ?y } order by ?y" 
			s2 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:dates ?y } order by ?y" 
			s3 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:datetimes ?y } order by ?y" 
			s4 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:decimals ?y } order by ?y" 
			s5 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:doubles ?y } order by ?y" 
			s6 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:floats ?y } order by ?y" 
			s7 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:integers ?y } order by ?y" 
			s8 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:strings ?y } order by ?y" 
			s9 "prefix sb: <http://seabass.foo/> select ?y where { ?x sb:times ?y } order by ?y" ]
		(is (= "false" 				(incanter/sel (bounce s1 m) :cols :y :rows 0)))	
		(is (= "true" 				(incanter/sel (bounce s1 m) :cols :y :rows 1)))
		(is (= -16098156000000 	(incanter/sel (bounce s2 m) :cols :y :rows 0)))	
		(is (= 239605200000 		(incanter/sel (bounce s2 m) :cols :y :rows 1)))
		(is (= -16098140964000 		(incanter/sel (bounce s3 m) :cols :y :rows 0)))	
		(is (= 239653200000 			(incanter/sel (bounce s3 m) :cols :y :rows 1)))
		(is (= -22.222 				(incanter/sel (bounce s4 m) :cols :y :rows 0)))	
		(is (= 22.222			 		(incanter/sel (bounce s4 m) :cols :y :rows 1)))
		(is (= -99.999 			(incanter/sel (bounce s5 m) :cols :y :rows 0)))	
		(is (= 99.999				(incanter/sel (bounce s5 m) :cols :y :rows 1)))
		(is (= -11 						(.intValue (incanter/sel (bounce s6 m) :cols :y :rows 0))))
		(is (= 11			 			(.intValue (incanter/sel (bounce s6 m) :cols :y :rows 1))))
		(is (= -12 					(incanter/sel (bounce s7 m) :cols :y :rows 0)))	
		(is (= 12 					(incanter/sel (bounce s7 m) :cols :y :rows 1)))
		(is (= "test"				 	(incanter/sel (bounce s8 m) :cols :y :rows 0)))	
		(is (= "test"			 		(incanter/sel (bounce s8 m) :cols :y :rows 1)))
		(is (= -6564000			(incanter/sel (bounce s9 m) :cols :y :rows 0)))	
		(is (= 66000000 		(incanter/sel (bounce s9 m) :cols :y :rows 1))) ))
		
(deftest builtin-test
	(let [ m (build "data/test.nt" "data/test.rules")
		s1 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-date ?y }"
		s2 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-datetime ?y }"
		s3 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-time ?y }" 
		s4 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-date-fail ?y }"
		s5 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-datetime-fail ?y }"
		s6 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-time-fail ?y }"
		s7 "prefix sb: <http://seabass.foo/>  select ?x ?y where { ?x sb:closeTo-time-diff ?y }" ]
		(is (= 2 (incanter/nrow (bounce s1 m))))
		(is (= 2 (incanter/nrow (bounce s2 m))))
		(is (= 2 (incanter/nrow (bounce s3 m))))	
		(is (= 0 (incanter/nrow (bounce s4 m))))
		(is (= 0 (incanter/nrow (bounce s5 m))))
		(is (= 0 (incanter/nrow (bounce s6 m))))
		(is (= 2 (incanter/nrow (bounce s7 m))))	))
		
(deftest stash-test
	(let [	m	(build "data/test.nt")
			n  (build (stash m "data/stash-test.nt"))	
			o	(build (stash n "data/stash-test-2.nt")) ]
		(is (.isIsomorphicWith m n))	
		(is (.isIsomorphicWith n o)) ))