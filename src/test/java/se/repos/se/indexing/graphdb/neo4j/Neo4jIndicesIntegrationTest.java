package se.repos.se.indexing.graphdb.neo4j;

import static org.junit.Assert.*;

import javax.ws.rs.client.WebTarget;

import org.junit.Test;

public class Neo4jIndicesIntegrationTest {

	@Test
	public void test() {
		Neo4jClientJaxrsProvider neoProvider = new Neo4jClientJaxrsProvider();
		
		Neo4jIndices neoIndices = new Neo4jIndices(neoProvider);
		
		WebTarget maps = neoIndices.getMaps();
	}

}
