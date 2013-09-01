(ns seabass.test.core
  (:import [com.hp.hpl.jena.rdf.model Model])
  (:use [seabass.core] :reload)
  (:use [clojure.test])
  (:use [clojure.java.io]))

(defn sb [q](str "prefix sb: <http://seabass.foo/> " q))
(defn sea [term] (str "http://seabass.foo/" term))

(deftest local-test
  (let [m  (build "data/test.ttl" ["data/test.nt" "N-TRIPLES"]) 
	s1 "select distinct ?p where { ?s ?p ?o }"
	s2 (sb "select ?x ?y where { ?x sb:neighbor ?y }")
	a1 (sb "ask {sb:olivia sb:caught sb:carl}")
	a2 (sb "ask {sb:carl sb:caught sb:olivia}")
	c1 (sb "construct { ?x sb:neighbor ?y }
                { ?p sb:caught ?x . ?p sb:caught ?y .
                filter( ?x != ?y )}")]
    (->> m (bounce s1) :data count (== 18) is)
    (->> m (ask a1) is)
    (->> m (ask a2) not is)
    (->> m (bounce s2) :data count (== 0) is)
    (->> m (pull c1) (bounce s2) :data count (== 2) is)
    (->> m (pull c1) (build m) (bounce s1) :data count (== 19) is)))
    
(deftest file-test
  (let [m (build (file "./data/test.ttl") (file "./data/test.nt"))
	s1 "select distinct ?p where { ?s ?p ?o }"
	s2 (sb "select ?x ?y where { ?x sb:neighbor ?y }")
	a1 (sb "ask {sb:olivia sb:caught sb:carl}")
	a2 (sb "ask {sb:carl sb:caught sb:olivia}")
	c1 (sb "construct { ?x sb:neighbor ?y } { ?p sb:caught ?x . ?p sb:caught ?y . filter( ?x != ?y )}")]
    (->> m (bounce s1) :data count (== 18) is)
    (->> m (ask a1) is)
    (->> m (ask a2) not is)
    (->> m (bounce s2) :data count (== 0) is)
    (->> m (pull c1) (bounce s2) :data count (== 2) is)
    (->> m (pull c1) (build m) (bounce s1) :data count (== 19) is)))

(comment 
(deftest remote-test
  (let [s1 "select ?x where { ?x a <http://seabass.foo/Fish>  } limit 10"
	s2 "select ?p where { ?s ?p ?o }"
	c1 "construct {?x a <http://seabass.foo/Fish>} where { ?x a <http://www4.wiwiss.fu-berlin.de/factbook/ns#Country>}"
	endpoint "http://www4.wiwiss.fu-berlin.de/factbook/sparql"
	remote-xml "http://id.southampton.ac.uk/dataset/apps/latest.rdf"
	remote-ttl "http://id.southampton.ac.uk/dataset/apps/latest.ttl"
	m (build "data/test.ttl" "data/test.nt")]
    (->> m (bounce s1) :data count (== 3) is)
    (->> endpoint (pull c1) (build m) (bounce s1) :data count (== 10) is)
    (->> [remote-xml "RDF/XML"] (pull c1) (build m) (bounce s2) :data count (< 0) is)    
    (->> [remote-ttl "TTL"] build (bounce s2) :data count (< 0) is))))

(deftest reasoning-test
  (let [ m  (build "data/test.ttl" "data/test.nt") 
	s1 (sb "select ?x ?y where { ?x sb:neighbor ?y }")
	r "data/test.rules" ]
    (->> m (bounce s1) :data count (== 0) is)
    (->> m (build r) (bounce s1) :data count (== 2) is)))

(deftest datatype-test
  (let [ m (build "data/test.nt")
	s1 (sb "select ?y where { ?x sb:booleans ?y } order by ?y") 
	s2 (sb "select ?y where { ?x sb:dates ?y } order by ?y") 
	s3 (sb "select ?y where { ?x sb:datetimes ?y } order by ?y")
	s4 (sb "select ?y where { ?x sb:decimals ?y } order by ?y")
	s5 (sb "select ?y where { ?x sb:doubles ?y } order by ?y")
	s6 (sb "select ?y where { ?x sb:floats ?y } order by ?y")
	s7 (sb "select ?y where { ?x sb:integers ?y } order by ?y")
	s8 (sb "select ?y where { ?x sb:strings ?y } order by ?y")
	s9 (sb "select ?y where { ?x sb:times ?y } order by ?y") ]
    (-> (bounce s1 m) :data (nth 0) :y is)
    (-> (bounce s1 m) :data (nth 1) :y not is)
    (is (== 239605200000 (-> (bounce s2 m) :data (nth 0) :y .asCalendar .getTimeInMillis)))
    (is (== -16097378400000 (-> (bounce s2 m) :data (nth 1) :y .asCalendar .getTimeInMillis)))
    (is (== 239653200000 (-> (bounce s3 m) :data (nth 0) :y .asCalendar .getTimeInMillis)))
    (is (== -16097363364000 (-> (bounce s3 m) :data (nth 1) :y .asCalendar .getTimeInMillis)))
    (is (== 22.222 (-> (bounce s4 m) :data (nth 0) :y)))	
    (is (== -22.222 (-> (bounce s4 m) :data (nth 1) :y)))
    (is (== 99.999 (-> (bounce s5 m) :data (nth 0) :y)))	
    (is (== -99.999 (-> (bounce s5 m) :data (nth 1) :y)))
    (is (== 11  (-> (bounce s6 m) :data (nth 0) :y .intValue)))
    (is (== -11  (-> (bounce s6 m) :data (nth 1) :y .intValue)))
    (is (== 12 (-> (bounce s7 m) :data (nth 0) :y)))	
    (is (== -12 (-> (bounce s7 m) :data (nth 1) :y)))
    (is (= "test" (-> (bounce s8 m) :data (nth 0) :y)))
    (is (= "test" (-> (bounce s8 m) :data (nth 1) :y)))
    (is (== 947887836000 (-> (bounce s9 m) :data (nth 0) :y .asCalendar .getTimeInMillis)))	
    (is (== 947960400000 (-> (bounce s9 m) :data (nth 1) :y .asCalendar .getTimeInMillis)))))

(deftest builtin-test
  (let [ m (build "data/test.nt" "data/test.rules")
	s1 (sb "select ?x ?y where { ?x sb:closeTo-date ?y }")
	s2 (sb "select ?x ?y where { ?x sb:closeTo-datetime ?y }")
	s3 (sb "select ?x ?y where { ?x sb:closeTo-time ?y }")
	s4 (sb "select ?x ?y where { ?x sb:closeTo-date-fail ?y }")
	s5 (sb "select ?x ?y where { ?x sb:closeTo-datetime-fail ?y }")
	s6 (sb "select ?x ?y where { ?x sb:closeTo-time-fail ?y }")
	s7 (sb "select ?x ?y where { ?x sb:closeTo-time-diff ?y }") ]
    (is (== 2 (-> (bounce s1 m) :data count)))
    (is (== 2 (-> (bounce s2 m) :data count)))
    (is (== 2 (-> (bounce s3 m) :data count)))
    (is (== 0 (-> (bounce s4 m) :data count)))
    (is (== 0 (-> (bounce s5 m) :data count)))
    (is (== 0 (-> (bounce s6 m) :data count)))
    (is (== 2 (-> (bounce s7 m) :data count)))))

(deftest stash-test
  (let [m (build "data/test.nt")
	n (build (stash m "data/stash-test.nt"))]
    (is (.isIsomorphicWith m n)) ))

(deftest prefix-test
  (let [m (build "data/test.ttl")
	q1 "select ?x { ?x rdf:type rdf:Property }"
	q2 "select ?x { ?x rdfs:domain ?y }"
	q3 "select ?x { ?x rdfs:range xsd:integer }"
	q4 "select ?x { ?x rdf:type owl:Class }"]
    (is (= 2 (-> (bounce q1 m) :data count)))
    (is (= 2 (-> (bounce q2 m) :data count)))
    (is (= 1 (-> (bounce q3 m) :data count)))
    (is (= 0 (-> (bounce q4 m) :data count)))))

(deftest optional-test
  (let [m (build "data/test.ttl")
        q (sb "select ?x ?y { ?x a sb:Fish . optional { ?x sb:caught ?y }}")]
    (is (= 0 (-> (bounce q m) :data count)))))

(deftest add-test
  (let [m (build)
        q1 (sb "select ?n {?x sb:caught sb:walter . ?x sb:name ?n }")
        q2 (sb "select ?w {?x sb:caught sb:water . ?x sb:weight ?w }")
        df (java.text.SimpleDateFormat. "yyyy-MM-dd")
        r1 (resource-fact (sea "walter") (sea "caught") (sea "carl"))
        r2 (resource-fact "_:x" (sea "caught") (sea "walter"))
        r3 (resource-fact "_:x" (sea "knows") "_:y")
        l1 (literal-fact (sea "walter") (sea "name") "Walter")
        l2 (literal-fact (sea "walter") (sea "weight") 200)
        l3 (literal-fact (sea "walter") (sea "born") (.parse df "1977-10-10"))
        l4 (literal-fact "_:x" (sea "name") "Jimmy")
        l5 (literal-fact "_:y" (sea "weight") 100.5)]
    (is (= 1 (.size m)))
    (push m r1)
    (is (= 2 (.size m)))
    (push m r2 r3 l1 l2 l3)
    (apply push [m l4 l5])
    (is (= 9 (.size m)))
    (is (= "Jimmy" (-> (bounce q1 m) :data (nth 0) :n)))
    (is (= 0 (-> (bounce q2 m) :data count)))))
  