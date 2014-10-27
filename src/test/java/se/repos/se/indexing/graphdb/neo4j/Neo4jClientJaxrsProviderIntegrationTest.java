package se.repos.se.indexing.graphdb.neo4j;

import static org.junit.Assert.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.jayway.jsonpath.JsonPath;

public class Neo4jClientJaxrsProviderIntegrationTest {
	
	@Test
	public void testReadNode() {
		WebTarget neo = new Neo4jClientJaxrsProvider().get();
		
		Form form = new Form();
		form.param("id", "test_" + this.getClass().getName() + System.currentTimeMillis());
		
		Response created = neo.path("node").request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
		System.out.println("POST response: ");
		System.out.println(created.getHeaders());
		// readEntity also closes the stream
		Integer id = (Integer) JsonPath.read(created.readEntity(String.class), "$.metadata.id");
		
		Response read = neo.path("node/" + id).request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, read.getStatus());
		System.out.println("GET response: ");
		System.out.println(read.getHeaders());
		System.out.println(read.readEntity(String.class));
	}

}
