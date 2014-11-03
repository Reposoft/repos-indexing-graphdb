package se.repos.se.indexing.graphdb.neo4j;

import javax.inject.Named;
import javax.ws.rs.client.WebTarget;

public class Neo4jIndices {
	
	private Neo4jClientJaxrsProvider neo;

	public Neo4jIndices(@Named("neo4j") Neo4jClientJaxrsProvider neo) {
		this.neo = neo;
		createMaps();
		createContent();
	}
	
	void createMaps() {
		
	}
	
	void createContent() {
		
	}

	public WebTarget getMaps() {
		return null;
	}
	
}
