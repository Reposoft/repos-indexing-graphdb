package se.repos.indexing.graphdb;

public enum GraphRelationshipTypes /*implements org.neo4j.graphdb.RelationshipType*/ {
	
	/**
	 * From parent node to child node. Should have child number.
	 * @deprecated This mimics the XML schema. We should model afresh.
	 */
	PARENT_OF,
	
	/**
	 * Applies a patch and thus produces a new revision.
	 */
	APPLIES
	
}