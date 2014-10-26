package se.repos.se.indexing.graphdb.neo4j;

import java.util.Set;

import javax.inject.Provider;

import org.neo4j.rest.graphdb.RestAPI;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.IndexingItemProgress;

public class IndexingItemHandlerNeo4j implements IndexingItemHandler {

	private Provider<RestAPI> neo4jprovider;

	public IndexingItemHandlerNeo4j(Provider<RestAPI> neo4jprovider) {
		this.neo4jprovider = neo4jprovider;
	}
	
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return null;
	}

	@Override
	public void handle(IndexingItemProgress progress) {
		// TODO Auto-generated method stub

	}

}
