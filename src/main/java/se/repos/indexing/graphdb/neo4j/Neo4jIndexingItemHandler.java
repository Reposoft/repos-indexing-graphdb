package se.repos.indexing.graphdb.neo4j;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.graphdb.GraphConfiguration;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.xmlsource.TreeLocation;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

/**
 * Handles both Item and XML.
 * 
 * This class meets our fixed design for CMS-to-graph, i.e. predictable Graph insertions.
 * Analysis and best effort relations is done in a javascript layer (possibly a node CLI utility) where JSON response parsing is simpler.
 * 
 * Syntax choices made:
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
	
	private Provider<CypherTransaction> neo;

	private IdStrategy idStrategy;	
	
	private Set<String> authoringUnitElements;

	@Inject
	public Neo4jIndexingItemHandler(Provider<CypherTransaction> neo4j,
			IdStrategy idStrategy,
			@Named("config:se.repos.indexing.authoringUnitElements") Set<String> authoringUnitElements) {
		this.neo = neo4j;
		this.idStrategy = idStrategy;
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
		CmsRepository repo = progress.getRepository();
		IndexingDoc fields = progress.getFields();
		knowsRepo(fields, repo);
		
		String docid = (String) fields.getFieldValue("idhead");
		String patchid = (String) fields.getFieldValue("id");
		
		CypherTransaction tx = neo.get();
		
		// If we didn't have the mapid we could use CREATE UNIQUE here
		if (progress.getItem().isAdd()) {
			tx.addStatement("CREATE (doc:Document { id: docid }) RETURN doc")
				.prop("id", docid);
		}
		// We know that patch is unique (and there should be uniquness constraints to verify that)
		tx.addStatement("CREATE (patch:Patch { id: patchid, revt: revt }) RETURN patch")
			.prop(patchid, patchid);
		// I guess the above could be chained to avoid another lookup, but here we go
		tx.addStatement("MATCH (doc:Document),(patch:Patch)"
				+ " WHERE doc.id = docid AND patch.id = patchid"
				+ " CREATE (doc)-[r:MOD]->(patch) RETURN r")
			.prop("docid", docid)
			.prop("patchid", patchid);
		
		if (progress.getItem().isCopy()) {
			// Assumptions/limitations: Copy is not historical + copy has no modifications.
			CmsItemPath from = progress.getItem().getCopyFromPath();
			
			// Assume for now that copy is not historical
			//RepoRevision fromrev = progress.getItem().getCopyFromRevision();
			//String fromrevid = idStrategy.getId(repo, fromrev, from);
			String fromid = idStrategy.getIdHead(repo, from);
			
			String stat = (String) fields.getFieldValue("pathstat");
			if (!"A".equals(stat)) {
				logger.warn("Avoiding graph indexing for copy-with-modification {} {}", stat, from);
			} else {
				tx.addStatement("MATCH (from:Document),(to:Document)"
						+ " WHERE from.id = fromid AND to.id = docid"
						+ " CREATE (from)-[r:COPY]-(to) RETURN r")
					.prop("fromid", fromid)
					.prop("docid", docid);
			}
		}
		
		logger.info("Graph commit: {}", tx.run().json());
	}
	
	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields)
			throws XmlNotWellFormedException {
		CmsRepository repo = getKnownRepo(fields);

		//logger.trace("Graphdb content unit indexing can use: {}", fields.getFieldNames());

		String nodename = processedElement.getName();
		if (!authoringUnitElements.contains(nodename)) {
			logger.trace("Not an authoring unit: {}", nodename);
			return;
		}
		String contentMatchId = (String) fields.getFieldValue(GraphConfiguration.CONTENT_MATCH_FIELD);
		
		// We don't have the item's doc id here, but we could probably get it with some state sharing between this and item handler
		CmsItemPath docpath = new CmsItemPath((String) fields.getFieldValue("path"));
		RepoRevision docrev = new RepoRevision((Long) fields.getFieldValue("rev"), (Date) fields.getFieldValue("revt"));
		String docid = idStrategy.getIdHead(repo, docpath);
		String docidrev = idStrategy.getId(repo, docrev, docpath);
		
		TreeLocation location = processedElement.getLocation();
		
		CypherTransaction tx = neo.get();
		
		tx.addStatement("MATCH (patch:Patch { id: patchid })"
				+ " CREATE UNIQUE (patch)-[:SEES { location : location }]-(content:Content { id: contentid }) RETURN content");
		
		Object relation = tx.run().json();
		
		logger.trace("Response: {}", relation); // {"results":[{"columns":["content"],"data":[]}],"errors":[]}
		if (relation.toString().contains("\"data\":[{")) {
			logger.debug("Graph updated for {} {} checksum={}", nodename, location, contentMatchId);
		} else {
			logger.error("Failed to create a relation for {}->{} at {}. Got {}.", docid + docidrev, contentMatchId, location, relation);
		}
		
		// After this we should specialize SEES to KEEPS, ADDS, etc
	}
	
	@Override
	public void endDocument() {
	}
	
	// Can't know the repository instance from element indexing with current API, need it for ID creation
	private final Map<String, CmsRepository> repoids = new HashMap<String, CmsRepository>();
	
	/**
	 * Use a field present in both item and XML indexing to keep a reference to CmsRepository.
	 */
	private void knowsRepo(IndexingDoc fields, CmsRepository repository) {
		if (repository == null) {
			throw new IllegalArgumentException("Repository must be set");
		}
		if (fields.getFieldValue("repoid") == null) {
			throw new IllegalArgumentException("Field 'repoid' is required");
		}
		repoids.put((String) fields.getFieldValue("repoid"), repository);
	}
	
	private CmsRepository getKnownRepo(IndexingDoc fields) {
		Object id = fields.getFieldValue("repoid");
		CmsRepository repo = repoids.get(id);
		if (repo == null) {
			throw new IllegalArgumentException("No repository known for repoid '" + id + "'. Document not indexed yet?");
		}
		return repo;
	}

}
