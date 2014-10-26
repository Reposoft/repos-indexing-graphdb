package se.repos.se.indexing.graphdb.neo4j.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import se.repos.se.indexing.graphdb.neo4j.RelationshipTypes;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class Neo4jXmlIndexFieldExtraction implements XmlIndexFieldExtraction {

	public final Logger logger = LoggerFactory.getLogger(this.getClass());

	private Provider<RestAPI> neo4jprovider;	
	
	// caching
	private Map<XmlSourceElement, Node> created = new HashMap<XmlSourceElement, Node>();

	public Neo4jXmlIndexFieldExtraction(Provider<RestAPI> neo4jprovider) {
		this.neo4jprovider = neo4jprovider;
	}

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields)
			throws XmlNotWellFormedException {
		RestAPI graphDb = neo4jprovider.get();

		logger.info("neo4j can use fields {}", fields.getFieldNames());

		String id = (String) fields.getFieldValue("id");
		String nodename = (String) fields.getFieldValue("name");
		
		// create the node for the element
		Transaction tx = graphDb.beginTx();
		Map<String, Object> nodeParams = new HashMap<String, Object>();
		nodeParams.put("id", id);
		nodeParams.put("name", nodename);
		nodeParams.put("c_sha1_source_reuse", fields.getFieldValue("c_sha1_source_reuse"));
		nodeParams.put("depth", fields.getFieldValue("depth"));
		
		Node node  = graphDb.createNode(nodeParams);
		if (node == null) {
			throw new RuntimeException("Failed to create node " + id);
		}
		created.put(processedElement, node); // we'll have to clear the cache cleverly for this to scale
		
		// set node name as label, makes http://localhost:7474/browser/ more interesting
		node.addLabel(DynamicLabel.label(nodename));
		
		if (processedElement.isRoot()) {
			
		} else {
			Node parent = created.get(processedElement.getParent());
			if (parent == null) {
				throw new RuntimeException("Parent node not known at " + id);
			}
			
			// create relations from parent to child
			Map<String, Object> relParams = new HashMap<String, Object>();
			relParams.put("position", fields.getFieldValue("position"));
			graphDb.createRelationship(parent, node, RelationshipTypes.PARENT_OF, relParams);
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
