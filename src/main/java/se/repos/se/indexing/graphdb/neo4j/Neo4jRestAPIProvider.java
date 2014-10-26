package se.repos.se.indexing.graphdb.neo4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Provider;

import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jRestAPIProvider implements Provider<RestAPI> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	RestAPI graphDb = null;
	
	@Override
	public RestAPI get() {
		if (graphDb == null) {
			create();
		}
		return graphDb;
	}
	
	// http://thought-bytes.blogspot.se/2013/07/getting-started-with-neo4j-java-rest-heroku.html
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void create() {
		graphDb = new RestAPIFacade("http://localhost:7474/db/data");

		QueryEngine engine = new RestCypherQueryEngine(graphDb);
		QueryResult<Map<String, Object>> result = engine.query(
				"start n=node(*) return count(n) as total",
				Collections.EMPTY_MAP);
		Iterator<Map<String, Object>> iterator = result.iterator();
		if (iterator.hasNext()) {
			Map<String, Object> row = iterator.next();
			logger.trace("Connected to neo4j. Got {} nodes already.",
					row.get("total"));
		}
	}

}
