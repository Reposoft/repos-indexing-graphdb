package se.repos.se.indexing.graphdb.neo4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingDoc;
import se.repos.se.indexing.graphdb.GraphLabels;
import se.repos.se.indexing.graphdb.GraphRelationshipTypes;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class Neo4jIndexingItemXmlElementHandler implements XmlIndexFieldExtraction {

	public final Logger logger = LoggerFactory.getLogger(this.getClass());

	private Provider<RestAPI> neo4jprovider;	
	
	// caching
	private Map<XmlSourceElement, Node> created = new HashMap<XmlSourceElement, Node>();

	@Inject
	public Neo4jIndexingItemXmlElementHandler(Provider<RestAPI> neo4jprovider) {
		this.neo4jprovider = neo4jprovider;
	}

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields)
			throws XmlNotWellFormedException {
		RestAPI graphDb = neo4jprovider.get();

		logger.trace("Graphdb content unit indexing can use: {}", fields.getFieldNames());

		String xmlid = (String) fields.getFieldValue("id");
		String nodename = (String) fields.getFieldValue("name");
		String contentSha1 = (String) fields.getFieldValue("c_sha1_source_reuse");
		
		// create the node for the element
		Transaction tx = graphDb.beginTx();
		
		Map<String, Object> contentNodeP = new HashMap<String, Object>();
		contentNodeP.put("id", contentSha1);
		
		Node contentNode  = graphDb.createNode(contentNodeP);
		if (contentNode == null) {
			throw new RuntimeException("Failed to create node " + xmlid);
		}
		contentNode.addLabel(GraphLabels.Content);
		
		// We're not necessarily indexing the "model" because it might be in search index
		// This represents generic XML modelled 
		Map<String, Object> modelNodeP = new HashMap<String, Object>();
		modelNodeP.put("id", xmlid);
		modelNodeP.put("type", nodename);
		modelNodeP.put("body", ""); // TODO only on reuse level tags, not above and not below
		
		Node modelNode = graphDb.createNode(modelNodeP);
		modelNode.addLabel(GraphLabels.Model);
		
		// TODO The below should be the modelled as the Map, but should we also index relations?
		// We can not index relations on content nodes if they are only the unit, not the children.
		
		created.put(processedElement, contentNode); // we'll have to clear the cache cleverly for this to scale
		
		if (processedElement.isRoot()) {
			
		} else {
			Node parent = created.get(processedElement.getParent());
			if (parent == null) {
				throw new RuntimeException("Parent node not known at " + xmlid);
			}
			
			// create relations from parent to child
			Map<String, Object> relParams = new HashMap<String, Object>();
			relParams.put("position", fields.getFieldValue("position"));
			graphDb.createRelationship(parent, contentNode, GraphRelationshipTypes.PARENT_OF, relParams);
		}
		
		tx.success();
		tx.finish();
	}

	@Override
	public void endDocument() {
		logger.info("neo4j end doc");
		created.clear();
	}

}
