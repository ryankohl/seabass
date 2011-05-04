(ns seabass.utils
	(:import [com.hp.hpl.jena.datatypes.xsd XSDDatatype])	)
	
(defn valid-dt? [dt val]
	(condp = dt
		"boolean" 		(.isValid XSDDatatype/XSDboolean val)
		"date"			(.isValid XSDDatatype/XSDdate val)
		"datetime"		(.isValid XSDDatatype/XSDdateTime val)
		"decimal"		(.isValid XSDDatatype/XSDdecimal val)
		"double"		(.isValid XSDDatatype/XSDdouble val)
		"duration"		(.isValid XSDDatatype/XSDduration val)
		"float"			(.isValid XSDDatatype/XSDfloat val)
		"integer"		(.isValid XSDDatatype/XSDinteger val)
		"string"			(.isValid XSDDatatype/XSDstring val)
		"time"			(.isValid XSDDatatype/XSDtime val)
		nil	))