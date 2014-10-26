package se.repos.se.indexing.graphdb.neo4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.se.indexing.graphdb.GraphLabels;
import se.repos.se.indexing.graphdb.GraphRelationshipTypes;

public class Neo4jIndexingItemHandler implements IndexingItemHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private Provider<RestAPI> neo4jprovider;

	@Inject
	public Neo4jIndexingItemHandler(Provider<RestAPI> neo4jprovider) {
		logger.info("Initialized");
		this.neo4jprovider = neo4jprovider;
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
	private Map<String, Node> maps = new HashMap<String, Node>();
	
	/**
	 * Cooperates with {@link Neo4jIndexingItemXmlElementHandler}.
	 */
	public void handleStructuredData(IndexingItemProgress progress) {
		IndexingDoc fields = progress.getFields();
		
		RestAPI db = neo4jprovider.get();
		
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
	}

}
