package se.repos.se.indexing.graphdb.neo4j;

import org.neo4j.graphdb.RelationshipType;

public enum RelationshipTypes implements RelationshipType {
	/**
	 * From parent node to child node. Should have child number.
	 * TODO should it be PARENTOF?
	 */
	PARENT_OF
	
}