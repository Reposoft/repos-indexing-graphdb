package se.repos.indexing.graphdb.neo4j;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import com.jayway.jsonpath.JsonPath;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.graphdb.GraphLabels;
import se.repos.indexing.graphdb.GraphRelationshipTypes;
import se.repos.indexing.item.IndexingItemProgress;


/**
 * 
 * Ruled out http://neo4j.com/docs/stable/batchinsert-examples.html because it seems to need a lock on the DB dir.
 * 
 * Ruled out http://neo4j.com/docs/2.1.5/rest-api-nodes.html because it can't create node with a label, so uniquness would be enforced too late.
 * 
 * Ruled out http://neo4j.com/docs/2.1.5/rest-api-batch-ops.html for the same reason.
 * 
 * We could use http://neo4j.com/docs/2.1.5/rest-api-unique-indexes.html#rest-api-get-or-create-unique-node-create if all IDs, across labels, are unique - which would feel good anyway.
 * That model appears dated however, according to http://neo4j.com/docs/2.1.5/transactions-unique-nodes.html#transactions-get-or-create,
 * while http://neo4j.com/docs/2.1.5/rest-api-schema-constraints.html#rest-api-create-uniqueness-constraint is recommended.
 * The bad news is that this brings us back to how to create node with label.
 * For that we'll probably need to use Cypher.
 * 
 * Worth to note that the Java APIs they refer to in the docs are GraphDatabaseService from http://neo4j.com/docs/2.1.5/javadocs/org/neo4j/graphdb/factory/GraphDatabaseFactory.html,
 * which uses the local db dir instead of the HTTP interface.
 */
public class Neo4jIndexingItemHandler implements IndexingItemHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private WebTarget neo;

	@Inject
	public Neo4jIndexingItemHandler(@Named("neo4j") WebTarget neo4j) {
		this.neo = neo4j;
	}
	
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null; // TBD
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		if (progress.getItem() == null || !progress.getItem().isFile()) {
			logger.debug("Ignoring {}", progress);
			return;
		}
		Collection<Object> flag = progress.getFields().getFieldValues("flag");
		if (flag == null) {
			logger.debug("Only graphing if there is a flag");
			return;
		}
		if (flag.contains("hasxml")) {
			handleStructuredData(progress);
		} else {
			logger.debug("Not a recognized flag, graphing skipped");
		}
	}
	
	// Failed to use getOrCreateNode so we're caching here instead
	//private Map<String, Node> maps = new HashMap<String, Node>();
	
	/**
	 * Cooperates with {@link Neo4jIndexingItemXmlElementHandler}.
	 */
	public void handleStructuredData(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		
		// Just test the JAX-RS API
		WebTarget node = neo.path("node");
		
		Form mapForm = new Form();
		mapForm.param("id", (String) fields.getFieldValue("idhead"));
		
		// Maybe not needed when cypher queries maintain uniqueness, but obviously we should have this in a production setup
//		Response mapUniqueness = neo.path("schema/constraint/Map/uniqueness/")
//				.request(MediaType.APPLICATION_JSON)
//			    .post(Entity.entity("{\"property_keys\":[\"id\"]}", MediaType.APPLICATION_JSON));
//		System.out.println("Uniqueness response, status " + mapUniqueness.getStatus() + ": ");
//		System.out.println(mapUniqueness.getHeaders());
//		System.out.println(mapUniqueness.readEntity(String.class));
		
		String map = "MERGE (map:Map { id: '{}' }) RETURN map";
		String mapRevision = "MERGE (mapr:MapRevision { id: '{}', revt: '{}' }) RETURN mapr"; // TODO could be unique
		String mapRevisionRelation = "MATCH (map:Map),(mapr:MapRevision)"
				+ " WHERE map.id = '{}' AND mapr.id = '{}'"
				+ " CREATE (map)-[r:HAS]->(mapr) RETURN r";
		
		String response = Neo4jClientJaxrsProvider.runCypherTransaction(neo,
				MessageFormatter.format(map, fields.getFieldValue("idhead")).getMessage()
				,MessageFormatter.format(mapRevision, fields.getFieldValue("id"), fields.getFieldValue("revt")).getMessage()
				,MessageFormatter.format(mapRevisionRelation, fields.getFieldValue("idhead"), fields.getFieldValue("id")).getMessage()
				);
		System.out.println(response);
	}

}
