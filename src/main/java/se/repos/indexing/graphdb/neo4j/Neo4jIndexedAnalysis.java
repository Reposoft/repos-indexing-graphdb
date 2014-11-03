package se.repos.indexing.graphdb.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.scheduling.MarkerWhenIdle;

public class Neo4jIndexedAnalysis implements MarkerWhenIdle {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			add(Neo4jIndexingItemHandler.class);
		}};
	}	
	
	@Override
	public void ignore() {
		logger.debug("saw ignore");
	}

	@Override
	public void trigger() {
		logger.debug("saw trigger");
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		logger.debug("Graph indexing done. Post-analysis can start now for revision {}.", progress.getRevision());
	}

}
