package se.repos.indexing.graphdb.neo4j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.graphdb.neo4j.Neo4jClientJaxrsProvider;
import se.repos.indexing.graphdb.neo4j.Neo4jIndexingItemHandler;
import se.repos.indexing.graphdb.neo4j.Neo4jIndexingItemXmlElementHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

public class Neo4jIndexingItemHandlerIntegrationTest {

	@Test
	public void test() {
		Neo4jClientJaxrsProvider neoProvider = new Neo4jClientJaxrsProvider();
		Neo4jIndexingItemHandler handler = new Neo4jIndexingItemHandler(neoProvider.get());
		
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(progress.getFields()).thenReturn(doc);
		when(progress.getItem()).thenReturn(item);
		when(item.isFile()).thenReturn(true);
		
		doc.addField("flag", "hasxml");
		doc.addField("id", "some/file.xml@01");
		doc.addField("idhead", "some/file.xml");
		doc.addField("revt", System.currentTimeMillis());
		handler.handle(progress);
		
		// Nodes
		Set<String> authoringUnitElements = new HashSet<String>(Arrays.asList(
				// techdoc
				"p","title","figuretext",
				// checksheets
				"rule"));
		Neo4jIndexingItemXmlElementHandler xml = new Neo4jIndexingItemXmlElementHandler(neoProvider.get(), authoringUnitElements);
		
		// now revision 2
		//doc.addField("id", "some/file.xml@02");
		//handler.handle(progress);
		
		
	}

}
