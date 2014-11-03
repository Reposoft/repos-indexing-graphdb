package se.repos.indexing.graphdb.neo4j;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		String xmlid = (String) fields.getFieldValue("id");
		String nodename = (String) fields.getFieldValue("name");
		String contentSha1 = (String) fields.getFieldValue("c_sha1_source_reuse");
		TreeLocation location = processedElement.getLocation();
		
	}

	@Override
	public void endDocument() {
	}
	
	

}
