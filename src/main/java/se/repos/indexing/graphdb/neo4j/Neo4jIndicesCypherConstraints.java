package se.repos.indexing.graphdb.neo4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Neo4jIndicesCypherConstraints implements Neo4jIndices {
	
	private WebTarget neo;

	@Inject
	public Neo4jIndicesCypherConstraints(@Named("neo4j") WebTarget neo) {
		this.neo = neo;
		createMaps();
		createContent();
	}
	
	void createMaps() {
		// Maybe not needed when cypher queries maintain uniqueness, but obviously we should have this in a production setup
		Response mapUniqueness = neo.path("schema/constraint/Map/uniqueness/")
			.request(MediaType.APPLICATION_JSON)
			    .post(Entity.entity("{\"property_keys\":[\"id\"]}", MediaType.APPLICATION_JSON));
		System.out.println("Uniqueness response, status " + mapUniqueness.getStatus() + ": ");
		System.out.println(mapUniqueness.getHeaders());
		System.out.println(mapUniqueness.readEntity(String.class));
	}
	
	void createContent() {
		// Maybe not needed when cypher queries maintain uniqueness, but obviously we should have this in a production setup
		Response mapUniqueness = neo.path("schema/constraint/Content/uniqueness/")
			.request(MediaType.APPLICATION_JSON)
			    .post(Entity.entity("{\"property_keys\":[\"id\"]}", MediaType.APPLICATION_JSON));
		System.out.println("Uniqueness response, status " + mapUniqueness.getStatus() + ": ");
		System.out.println(mapUniqueness.getHeaders());
		System.out.println(mapUniqueness.readEntity(String.class));		
	}
	
}
