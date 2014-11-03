package se.repos.se.indexing.graphdb.neo4j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
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
		
		// now revision 2
		//doc.addField("id", "some/file.xml@02");
		//handler.handle(progress);
		
		
	}

}
