package se.repos.indexing.graphdb.neo4j;

import javax.inject.Provider;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jClientJaxrsProvider implements Provider<WebTarget> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jClientJaxrsProvider.class);
	
	public WebTarget get() {
		Client client = ClientBuilder.newClient();
		return client.target("http://localhost:7474/db/data");
	}
	
}
