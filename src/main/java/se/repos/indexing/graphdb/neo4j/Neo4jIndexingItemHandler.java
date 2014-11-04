package se.repos.indexing.graphdb.neo4j;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.xmlsource.TreeLocation;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Handles both Item and XML.
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
// Add this to do item and xml from same instance: @javax.inject.Singleton
public class Neo4jIndexingItemHandler implements IndexingItemHandler, XmlIndexFieldExtraction {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jIndexingItemHandler.class);
	
	private WebTarget neo;
	
	private Set<String> authoringUnitElements;

	@Inject
	public Neo4jIndexingItemHandler(@Named("neo4j") WebTarget neo4j,
			@Named("config:se.repos.indexing.authoringUnitElements") Set<String> authoringUnitElements) {
		this.neo = neo4j;
		this.authoringUnitElements = authoringUnitElements;
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
				
		String map = "MERGE (map:Map { id: '{}' }) RETURN map";
		String patch = "MERGE (patch:Patch { id: '{}', revt: '{}' }) RETURN patch"; // TODO could be unique
		String patchRelation = "MATCH (map:Map),(patch:Patch)"
				+ " WHERE map.id = '{}' AND patch.id = '{}'"
				+ " CREATE (map)-[r:HAS]->(patch) RETURN r";
		
		String mapid = (String) fields.getFieldValue("idhead");
		String patchid = (String) fields.getFieldValue("id");
		String response = runCypherTransaction(neo,
				MessageFormatter.format(map, mapid).getMessage()
				,MessageFormatter.format(patch, patchid, fields.getFieldValue("revt")).getMessage()
				,MessageFormatter.format(patchRelation, mapid, patchid).getMessage()
				);
		logger.debug("Patch creation {}->{} result: {}", mapid, patchid, response);
		
		if (progress.getItem().isCopy()) {
			// Assumptions/limitations: Copy is not historical + copy has no modifications.
			CmsItemPath from = progress.getItem().getCopyFromPath();
			
		}
	}
	
	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields)
			throws XmlNotWellFormedException {

		//logger.trace("Graphdb content unit indexing can use: {}", fields.getFieldNames());

		String nodename = processedElement.getName();
		if (!authoringUnitElements.contains(nodename)) {
			logger.trace("Not an authoring unit: {}", nodename);
			return;
		}
		String contentSha1 = (String) fields.getFieldValue("c_sha1_source_reuse");
		Long rev = (Long) fields.getFieldValue("rev");
		
		// We don't have the item's doc id here, but we could probably get it with some state sharing between this and item handler
		String mapid = "" + fields.getFieldValue("repoid") + ((String) fields.getFieldValue("path")).replace(" ", "%20"); // ids use an urlencoded path
		String mapidrev = "@" + String.format("%010d", rev); // from IdStrategyDefault
		
		TreeLocation location = processedElement.getLocation();
		
		String patchToChecksum = "MATCH (patch:Patch { id: '{}' })"
				+ " CREATE UNIQUE (patch)-[:SEES { location : '{}' }]-(content:Content { id: '{}'}) RETURN content";
		
		String relation = runCypherTransaction(neo, 
				MessageFormatter.arrayFormat(patchToChecksum,  new Object[]{ mapid + mapidrev, location, contentSha1 }).getMessage());
		
		logger.trace("Response: {}", relation); // {"results":[{"columns":["content"],"data":[]}],"errors":[]}
		if (relation.contains("\"data\":[{")) {
			logger.debug("Graph updated for {} {} checksum={}", nodename, location, contentSha1);
		} else {
			logger.error("Failed to create a relation for {}->{} at {}. Got {}.", mapid + mapidrev, contentSha1, location, relation);
		}
		
		// After this we should specialize SEES to KEEPS, ADDS, etc
	}

	@Override
	public void endDocument() {
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
