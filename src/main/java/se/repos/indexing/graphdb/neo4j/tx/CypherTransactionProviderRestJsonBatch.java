package se.repos.indexing.graphdb.neo4j.tx;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.indexing.graphdb.neo4j.Cypher;
import se.repos.indexing.graphdb.neo4j.CypherTransaction;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

public class CypherTransactionProviderRestJsonBatch implements Provider<CypherTransaction> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Run everything as a single transaction.
	 */
	private static final String SERVICE = "transaction/commit/";
	
	protected static final DateFormat CYPHER_JSON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static {
		CYPHER_JSON_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	private WebTarget neo;

	public CypherTransactionProviderRestJsonBatch(@Named("neo4j") WebTarget neo) {
		this.neo = neo;
	}

	@Override
	public CypherTransaction get() {
		return new Tx();
	}
	
	private class Tx implements CypherTransaction {

		private LinkedList<CypherJSONObject> statements = new LinkedList<CypherJSONObject>();
		
		private boolean closed = false;
		
		private void checkClosed() {
			if (closed) {
				throw new IllegalStateException("Transaction already closed");
			}
		}
		
		@Override
		public Cypher addStatement(String statement) {
			checkClosed();
			CypherJSONObject cypher = new CypherJSONObject(statement);
			statements.add(cypher);
			return cypher;
		}

		@Override
		public ReadContext run() {
			checkClosed();
			closed = true;
			JSONObject body = getRequestBody();
			Response response = neo.path(SERVICE)
					.request(MediaType.APPLICATION_JSON)
				    .post(Entity.entity(body.toJSONString(), MediaType.APPLICATION_JSON));
			logger.debug("Status {} from {}", response.getStatus(), body.toJSONString());
			String responseBody = response.readEntity(String.class);
			ReadContext responseJson = JsonPath.parse(responseBody);
			return responseJson;
		}

		@SuppressWarnings("unchecked")
		private JSONObject getRequestBody() {
			JSONObject body = new JSONObject();
			JSONArray batch = new JSONArray();
			body.put("statements", batch);
			for (CypherJSONObject stmt : statements) {
				batch.add(stmt.toJSON());
			}
			return body;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private class CypherJSONObject implements Cypher {

		private JSONObject statement;
		private JSONObject props;
		
		public CypherJSONObject(String statement) {
			this.statement = new JSONObject();
			this.statement.put("statement", statement);
			JSONObject parameters = new JSONObject();
			this.statement.put("parameters", parameters);
			this.props = new JSONObject();
			parameters.put("props", props);
		}

		private Cypher put(String name, Object value) {
			if (name == null) {
				throw new IllegalArgumentException("Property name is required");
			}
			if (value == null) {
				throw new IllegalArgumentException("Property value can not be null");
			}
			props.put(name, name);
			return this;
		}
		
		@Override
		public Cypher prop(String name, String value) {
			return put(name, value);
		}

		@Override
		public Cypher prop(String name, Long value) {
			return put(name, value);
		}

		@Override
		public Cypher prop(String name, Boolean value) {
			return put(name, value);
		}

		@Override
		public Cypher prop(String name, Date value) {
			return put(name, CYPHER_JSON_DATE_FORMAT.format(value));
		}
		
		private JSONObject toJSON() {
			return statement;
		}
		
	}
	
}
