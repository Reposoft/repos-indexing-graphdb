package se.repos.se.indexing.graphdb.neo4j;

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

import com.jayway.jsonpath.JsonPath;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.se.indexing.graphdb.GraphLabels;
import se.repos.se.indexing.graphdb.GraphRelationshipTypes;


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
		
		Response mapUniqueness = neo.path("schema/constraint/Map/uniqueness/")
				.request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.entity("{\"property_keys\":[\"id\"]}", MediaType.TEXT_PLAIN));
		System.out.println("Uniqueness response, status " + mapUniqueness.getStatus() + ": ");
		System.out.println(mapUniqueness.getHeaders());
		System.out.println(mapUniqueness.readEntity(String.class));
		
		//Response created = neo.path("index/node/maps?uniqueness=get_or_create")
		//Response created = neo.path("label/Map/node")
		//http://localhost:7474/db/data/index/node/people?uniqueness=get_or_create
		Response created = neo.path("node?uniqueness=get_or_create")
				.request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.entity(mapForm, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
		System.out.println("Map create POST response, status " + created.getStatus() + ": ");
		System.out.println(created.getHeaders());
		System.out.println(created.readEntity(String.class));
		
		
		// Just start using the index and it will be created automatically, says: http://neo4j.com/docs/2.1.5/rest-api-indexes.html#rest-api-create-node-index
		String uniquebody = "{" +
		"  \"key\" : \"id\"," +
		"  \"value\" : \"some/file5.txt\"," +
		"  \"properties\" : {" +
		"    \"id\" : \"some/file5.txt\"" +
		"  }" +
		"}";

		Response unique = neo.path("index/node/maps").queryParam("uniqueness","get_or_create")
				.request(MediaType.APPLICATION_JSON)
				// the query param gets urlencoded with .post
			    .post(Entity.entity(uniquebody, MediaType.TEXT_PLAIN));
		System.out.println("Get_or_create response, status " + unique.getStatus() + ": ");
		System.out.println(unique.getHeaders());
		System.out.println(unique.readEntity(String.class));

		// The above urlencodes the ? in the URL (I guess it is pretty non-standard to post with a query string) so try raw
		String url = "http://localhost:7474/db/data/index/node/maps?uniqueness=get_or_create";
		String json; 
		//json = postrawjson(uniquebody, url);
		
		// Try the same using batch instead
		String uniqueAndLabel = "["
				// The node
				// Apparently the uniqueness URL can't be combined with label creation using referral
				//+ "{\"method\":\"POST\", \"to\":\"/index/node/maps?uniqueness=get_or_create\","
				+ "{\"method\":\"POST\", \"to\":\"/node\","
				+ "\"id\":0, \"body\":" + uniquebody + "}"
				// a label, http://neo4j.com/docs/stable/rest-api-batch-ops.html#rest-api-refer-to-items-created-earlier-in-the-same-batch-job
				+ ",{\"method\":\"POST\", \"to\":\"{0}/labels\","
				+ "\"id\":1, \"body\":\"Map\"}"
						+ "]";
		System.out.println(uniqueAndLabel);
		//json = postrawjson(uniqueAndLabel, "http://localhost:7474/db/data/batch");
		Response batch = neo.path("batch")
				.request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.entity(uniqueAndLabel, MediaType.APPLICATION_JSON));
		System.out.println("Batch response, status " + batch.getStatus() + ": ");
		System.out.println(batch.getHeaders());
		System.out.println(batch.readEntity(String.class));
		
		
		//testBatch();				
		
		
		// readEntity also closes the stream
		//Integer id = (Integer) JsonPath.read(created.readEntity(String.class), "$.metadata.id");		
		
		// Preparation, depending on what we end up with this could be provider stuff
		//RestIndex<Node> mapIndex = db.getIndex("maps");
		//if (mapIndex == null) {
		//	logger.debug("Creating index 'maps'");
		//	Map<String,String> mapsConfig = new HashMap<String, String>();
		//	db.createIndex(Node.class, "maps", mapsConfig);
		//}
		//RestIndex<Node> mapIndex = db.getIndex("maps");
		// End preparation, start item processing
		
		Map<String, Object> mapNodeP = new HashMap<String, Object>();
		String mapId = (String) fields.getFieldValue("idhead");
		mapNodeP.put("id", mapId);
		
		
		
		/*
		Node mapNode;
		// TODO on Add delete or overwrite existing map, support incremental indexing
		//Node mapNode  = db.getOrCreateNode(mapIndex, "id", mapNodeP.get("id"), mapNodeP);
		if (maps.containsKey(mapId)) {
			mapNode = maps.get(mapId);
			logger.debug("In-memory cache had Map node {} for id {}", mapNode, mapId);
		} else {
			mapNode = db.createNode(mapNodeP);
			mapNode.addLabel(GraphLabels.Map);
			maps.put(mapId, mapNode);
			logger.info("Created new Map node {} for id {}", mapNode, mapId);
		}
		
		Map<String, Object> mapRevisionNodeP = new HashMap<String, Object>();
		mapRevisionNodeP.put("id", fields.getFieldValue("id"));
		Node mapRevisionNode  = db.createNode(mapRevisionNodeP);
		mapRevisionNode.addLabel(GraphLabels.MapRevision);
		
		Map<String, Object> commitP = new HashMap<String, Object>();
		commitP.put("rev", fields.getFieldValue("rev"));
		commitP.put("comment", fields.getFieldValue("revcomment")); // Or should we model a CommitRevision and add relation from there to MapRevision? Reduce redundancy.
		RestRelationship revisionRelationship = db.createRelationship(mapNode, mapRevisionNode, GraphRelationshipTypes.APPLIES, commitP); // Or reverse and call MODIFY?
		
		// TODO handle copy destination
		logger.debug("neo4j added {} {}", mapRevisionNode, revisionRelationship);
		*/
	}

	private String postrawjson(String body, String encodedUrl) {
		try {
			URL url = new URL(encodedUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setInstanceFollowRedirects(false); 
			connection.setRequestMethod("POST"); 
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", Integer.toString(body.getBytes().length));
			connection.setRequestProperty("Accept", "application/json");
			connection.setUseCaches(false);
	
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
			wr.writeBytes(body);
			wr.flush();
			wr.close();
			connection.disconnect();
			
			System.out.println("Result from raw JSON post " + connection.getResponseCode());
			InputStream response = connection.getInputStream();
			ByteArrayOutputStream to = new ByteArrayOutputStream();
			int b;
			while ((b = response.read()) > 0) {
				to.write(b);
			}
			response.close();
			System.out.println(to.toString("UTF-8"));
			return to.toString("UTF-8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void testBatch() {
		String batchbody = "[{" + 
				"\"method\" : \"POST\"," +
				"\"to\" : \"/node\"," +
				"\"body\" : {" +
				// just a regular attribute
				" \"labels\" : [\"Map\"]," +
				"  \"test\" : 1" +
				"}," +
				"\"id\" : 2," +
				// Not working:
				"\"labels\" : [\"Map\"]" +
				"}]";
		Response batch = neo.path("batch")
				.request(MediaType.APPLICATION_JSON_TYPE)
			    .post(Entity.entity(batchbody, MediaType.TEXT_PLAIN));
		System.out.println("Batch response, status " + batch.getStatus() + ": ");
		System.out.println(batch.getHeaders());
		System.out.println(batch.readEntity(String.class));
	}

}
