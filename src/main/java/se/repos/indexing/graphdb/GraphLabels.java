package se.repos.indexing.graphdb;

public enum GraphLabels /*implements org.neo4j.graphdb.Label*/ {

	/**
	 * A resource that has Content.
	 */
	Map,
	
	/**
	 * Still undecided if this points to Structure nodes that point to Structure|Content,
	 * or if this is a map with "pos" values (but ascii sortable instead of 1.2.3?)
	 */
	MapRevision,
	
	/**
	 * The "reuse" checksum for a content unit (a.k.a. authoring unit)
	 */
	Content, 
	
	/**
	 * The user edited model.
	 */
	Model
	
}
