package se.repos.se.indexing.graphdb.neo4j;

import javax.inject.Provider;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

public class Neo4jClientJaxrsProvider implements Provider<WebTarget> {

	public WebTarget get() {
		Client client = ClientBuilder.newClient();
		return client.target("http://localhost:7474/db/data");
	}
	
}
