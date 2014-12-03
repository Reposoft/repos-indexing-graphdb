package se.repos.indexing.graphdb.neo4j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.graphdb.neo4j.Neo4jClientJaxrsProvider;
import se.repos.indexing.graphdb.neo4j.Neo4jIndexingItemHandler;
import se.repos.indexing.graphdb.neo4j.tx.CypherTransactionProviderRestJsonBatch;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.twophases.IndexingDocIncrementalSolrj;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.indexing.IdStrategy;
import se.simonsoft.cms.item.indexing.IdStrategyDefault;
import se.simonsoft.cms.xmlsource.TreeLocation;
import se.simonsoft.cms.xmlsource.handler.XmlSourceElement;

public class Neo4jIndexingItemHandlerIntegrationTest {

	@Test
	public void test() {
		Neo4jClientJaxrsProvider neoProvider = new Neo4jClientJaxrsProvider();
		Provider<CypherTransaction> txProvider = new CypherTransactionProviderRestJsonBatch(neoProvider.get());
		IdStrategy idStrategy = new IdStrategyDefault();
		Set<String> authoringUnitElements = new HashSet<String>(Arrays.asList(
				// techdoc
				"p","title","figuretext",
				// checksheets
				"rule"));
		Neo4jIndexingItemHandler handler = new Neo4jIndexingItemHandler(
				txProvider,
				idStrategy,
				authoringUnitElements);
		
		IndexingItemProgress progress = mock(IndexingItemProgress.class);
		
		CmsChangesetItem item = mock(CmsChangesetItem.class);
		IndexingDoc doc = new IndexingDocIncrementalSolrj();
		when(progress.getRepository()).thenReturn(new CmsRepository("https://x.y/svn/r1"));
		when(progress.getFields()).thenReturn(doc);
		when(progress.getItem()).thenReturn(item);
		when(item.isFile()).thenReturn(true);
		when(item.isAdd()).thenReturn(true);
		
		doc.addField("flag", "hasxml");
		doc.setField("repoid", "r1");
		doc.addField("id", "some/file.xml@0000000001");
		doc.addField("idhead", "some/file.xml");
		doc.addField("path", "/some/file.xml");
		doc.addField("rev", 1L);
		doc.addField("revt", new Date());
		handler.handle(progress);
		
		// Nodes

		XmlSourceElement el1 = mock(XmlSourceElement.class);
		when(el1.getName()).thenReturn("p");
		when(el1.getLocation()).thenReturn(new TreeLocation("1.20.1.2"));
		IndexingDoc xml1 = new IndexingDocIncrementalSolrj();
		xml1.setField("repoid", "r1");
		xml1.setField("path", "/some/file.xml");
		xml1.setField("rev", 1L);
		xml1.setField("c_sha1_source_reuse", "aaaaaaa1");
		
		handler.extract(el1, xml1);
		
		// now revision 2
		//doc.addField("id", "some/file.xml@02");
		//handler.handle(progress);
		
		
	}

}
