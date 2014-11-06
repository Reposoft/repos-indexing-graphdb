package se.repos.indexing.graphdb.neo4j;

import java.util.Date;

/**
 * Controls our own type support for propeters.
 */
public interface Cypher {

	Cypher prop(String name, String value);
	
	Cypher prop(String name, Long value);
	
	Cypher prop(String name, Boolean value);
	
	Cypher prop(String name, Date value);
	
}
