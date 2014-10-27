package se.repos.se.indexing.graphdb.neo4j;

import static org.junit.Assert.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

public class Neo4jClientJaxrsProviderIntegrationTest {
	
	@Test
	public void testReadNode() {
		WebTarget neo = new Neo4jClientJaxrsProvider().get();
		Response response = neo.path("node/1").request(MediaType.APPLICATION_JSON).get();
		System.out.println(response);
	}

}
