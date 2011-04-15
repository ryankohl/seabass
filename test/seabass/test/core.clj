(ns seabass.test.core
	(:use [seabass.core] :reload)
	(:use [clojure.test])
	(:require 	[incanter.core :as incanter]	))
  
(deftest local-test
	(let [ m  (build "data/test.ttl" "data/test.nt") 
			s1 "select distinct ?p where { ?s ?p ?o }"
			s2 "prefix sb: <http://seabass.foo/> select ?x ?y where { ?x sb:neighbor ?y }"
			a1 "prefix sb: <http://seabass.foo/> ask {sb:olivia sb:caught sb:carl}"
			a2 "prefix sb: <http://seabass.foo/> ask {sb:carl sb:caught sb:olivia}"
			c1 "prefix sb: <http://seabass.foo/> construct { ?x sb:neighbor ?y } { ?p sb:caught ?x . ?p sb:caught ?y . filter( ?x != ?y )} "
			]
		(is (= 6 (incanter/nrow (bounce s1 m))))
		(is (ask a1 m))
		(is (not (ask a2 m)))
		(is (= 0 (incanter/nrow(bounce s2 m))))
		(is (= 2 (incanter/nrow (bounce s2 (pull c1 m)))))
		(is (= 7 (incanter/nrow (bounce s1 (build m (pull c1 m))))))	))
		
(deftest remote-test
	(let [	s1 "select ?x where { ?x a <http://seabass.foo/Fish>  } limit 10"
			c1 "construct {?x a <http://seabass.foo/Fish>} where { ?x a <http://www4.wiwiss.fu-berlin.de/factbook/ns#Country>}"
			endpoint "http://www4.wiwiss.fu-berlin.de/factbook/sparql"
			m (build "data/test.ttl" "data/test.nt")	]
		(is (= 3 (incanter/nrow (bounce s1 m))))
		(is (= 10 (incanter/nrow (bounce s1 (build m (pull c1 endpoint))))))	))
		
(deftest reasoning-test
	(let [ m  (build "data/test.ttl" "data/test.nt") 
			s1 "prefix sb: <http://seabass.foo/> select ?x ?y where { ?x sb:neighbor ?y }"
			r	"data/test.rules" ]
			(is (= 0 (incanter/nrow (bounce s1 m))))
			(is (= 2 (incanter/nrow (bounce s1 (build m r)))))	))