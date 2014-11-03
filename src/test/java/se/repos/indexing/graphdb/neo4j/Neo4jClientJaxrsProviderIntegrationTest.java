package se.repos.indexing.graphdb.neo4j;

import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import se.repos.indexing.graphdb.neo4j.Neo4jClientJaxrsProvider;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

public class Neo4jClientJaxrsProviderIntegrationTest {
	
	@Test
	public void testReadNode() {
		WebTarget neo = new Neo4jClientJaxrsProvider().get();
		
		Form form = new Form();
		form.param("id", "test_" + this.getClass().getName() + System.currentTimeMillis());
		form.param("samplekey", "samplevalue");
		
		Response created = neo.path("node").request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
		assertEquals(201, created.getStatus());
		// readEntity also closes the stream
		Integer id = (Integer) JsonPath.read(created.readEntity(String.class), "$.metadata.id");
		
		Response setlabel = neo.path("node/" + id + "/labels")
				.request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.json("\"Unittest\""));
		assertEquals("Got " + setlabel.getHeaders() + " " + setlabel.readEntity(String.class), 204, setlabel.getStatus());
		
		Response read = neo.path("node/" + id).request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, read.getStatus());
		System.out.println("GET response: ");
		System.out.println(read.getHeaders());
		
		ReadContext json = JsonPath.parse(read.readEntity(String.class));
		List<String> labels = json.read("$.metadata.labels");
		assertEquals(1, labels.size());
		assertEquals("Unittest", labels.get(0));
		assertEquals(form.asMap().get("id").get(0), json.read("$.data.id"));
	}

}
