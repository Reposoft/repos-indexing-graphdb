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
	
	/**
	 * @param cypher without double quotes, generated without user input
	 * @return neo4j REST response
	 */
	static String runCypherTransaction(WebTarget neo, String... cypher) {
		StringBuffer statements = new StringBuffer("{\"statements\" : [");
		for (int i = 0; i < cypher.length; i++) {
			if (i > 0) {
				statements.append(',');
			}
			statements.append("{\"statement\":\"").append(cypher[i]).append("\"}");
		}
		statements.append("]}");
		Response runAndCommit = neo.path("transaction/commit/")
				.request(MediaType.APPLICATION_JSON)
			    .post(Entity.entity(statements.toString(), MediaType.APPLICATION_JSON));
		logger.debug("Status {} from {}", runAndCommit.getStatus(), statements);
		String response = runAndCommit.readEntity(String.class);
		return response;
	}
	
	
}
