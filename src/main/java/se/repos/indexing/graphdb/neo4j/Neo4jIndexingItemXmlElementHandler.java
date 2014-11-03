package se.repos.indexing.graphdb.neo4j;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.simonsoft.cms.indexing.xml.XmlIndexFieldExtraction;
import se.simonsoft.cms.xmlsource.TreeLocation;
import se.simonsoft.cms.xmlsource.handler.XmlNotWellFormedException;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class Neo4jIndexingItemXmlElementHandler implements XmlIndexFieldExtraction {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private WebTarget neo;

	private Set<String> authoringUnitElements;

	@Inject
	public Neo4jIndexingItemXmlElementHandler(
			@Named("neo4j") WebTarget neo4j, 
			@Named("config:se.repos.indexing.authoringUnitElements") Set<String> authoringUnitElements) {
		this.neo = neo4j;
		this.authoringUnitElements = authoringUnitElements;
	}

	@Override
	public void extract(XmlSourceElement processedElement, IndexingDoc fields)
			throws XmlNotWellFormedException {

		logger.trace("Graphdb content unit indexing can use: {}", fields.getFieldNames());

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
		
		String maprToChecksum = "MATCH (mapr:MapRevision { id: '{}' })"
				+ " CREATE UNIQUE (mapr)-[:HAS { location : '{}' }]-(content:Content { id: '{}'}) RETURN content";
		
		String relation = Neo4jClientJaxrsProvider.runCypherTransaction(neo, 
				MessageFormatter.arrayFormat(maprToChecksum,  new Object[]{ mapid + mapidrev, location, contentSha1 }).getMessage());
		
		logger.trace("Response: {}", relation); // {"results":[{"columns":["content"],"data":[]}],"errors":[]}
		if (relation.contains("\"data\":[{")) {
			logger.debug("Graph updated for {} {} checksum={}", nodename, location, contentSha1);
		} else {
			logger.error("Failed to create a relation for {}->{} at {}. Got {}.", mapid + mapidrev, contentSha1, location, relation);
		}
		
		// After this we should specialize HAS to KEEPS, ADDS, etc
	}

	@Override
	public void endDocument() {
	}
	
	

}
