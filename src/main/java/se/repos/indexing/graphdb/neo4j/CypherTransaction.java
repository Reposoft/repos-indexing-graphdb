package se.repos.indexing.graphdb.neo4j;

import javax.inject.Provider;

import com.jayway.jsonpath.ReadContext;

/**
 * The new REST API http://neo4j.com/docs/milestone/rest-api-transactional.html
 * 
 * You want to inject a {@link Provider} of this.
 * 
 * API for now geared towards running a single batch of statements in a single short transaction.
 */
public interface CypherTransaction {

	/**
	 * @param statemet Cypher syntax
	 * @return support for setting named parameters, can be ignored for complete statements
	 */
	Cypher addStatement(String statemet);
	
	/**
	 * Runs the statements and parses the response.
	 * 
	 * @return Root of the returned JSON object.
	 */
	ReadContext run();
	
}
