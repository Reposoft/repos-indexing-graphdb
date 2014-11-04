package se.repos.indexing.graphdb.neo4j;

import static org.junit.Assert.*;

import javax.ws.rs.client.WebTarget;

import org.junit.Test;

import se.repos.indexing.graphdb.neo4j.Neo4jClientJaxrsProvider;
import se.repos.indexing.graphdb.neo4j.Neo4jIndicesCypherConstraints;

public class Neo4jIndicesIntegrationTest {

	@Test
	public void test() {
		Neo4jClientJaxrsProvider neoProvider = new Neo4jClientJaxrsProvider();
		
		Neo4jIndicesCypherConstraints neoIndices = new Neo4jIndicesCypherConstraints(neoProvider.get());
		
		
	}

}
