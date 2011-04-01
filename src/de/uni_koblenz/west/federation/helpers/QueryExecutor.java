/*
 * This file is part of RDF Federator.
 * Copyright 2010 Olaf Goerlitz
 * 
 * RDF Federator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * RDF Federator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with RDF Federator.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * RDF Federator uses libraries from the OpenRDF Sesame Project licensed 
 * under the Aduna BSD-style license. 
 */
package de.uni_koblenz.west.federation.helpers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.EmptyIteration;
import info.aduna.iteration.LookAheadIteration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.openrdf.cursor.Cursor;
//import org.openrdf.cursor.DelegatingCursor;
//import org.openrdf.cursor.EmptyCursor;
//import org.openrdf.http.client.TupleQueryClient;
//import org.openrdf.http.client.connections.HTTPConnectionPool;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.UnsupportedQueryLanguageException;
//import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.http.HTTPTupleQuery;
import org.openrdf.repository.sparql.SPARQLConnection;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for conveniently executing SPARQL queries.
 * 
 * Supports query evaluation using
 * - RepositoryConnection (repCon.prepareTupleQuery().evaluate() -> TupleResult)
 * - SailConnection       (sailCon.evaluate() -> Cursor)
 * - EvaluationStrategy   (strategy.evaluate() -> Cursor)
 * - HTTP endpoint        (HTTPTupleQuery.evaluate() -> TupleResult)
 * 
 * @author Olaf Goerlitz
 */
public final class QueryExecutor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryExecutor.class);
	
	private static final SPARQLParser parser = new SPARQLParser();
	
	private static final Map<String, SPARQLRepository> httpMap = new HashMap<String, SPARQLRepository>();
//	private static final Map<String, HTTPRepository> httpMap = new HashMap<String, HTTPRepository>();
//	private static final Map<String, HTTPConnectionPool> httpMap = new HashMap<String, HTTPConnectionPool>();
	
	/**
	 * Returns the size of the supplied result set.
	 * 
	 * @param result the result set to scan.
	 * @return the size of the result set (number of bindings)
	 */
//	public static int getSize(Cursor<BindingSet> result) {
	public static int getSize(CloseableIteration<? extends BindingSet, QueryEvaluationException> result) {
		List<BindingSet> bindings = asList(result);
		if (bindings != null)
			return bindings.size();
		return -1;
	}
	
	/**
	 * Evaluates the supplied query fragment.
	 * 
	 * @param eval the evaluation strategy to use.
	 * @param expr the query fragment to evaluate.
	 * @return the list of result bindings.
	 */
	public static List<BindingSet> eval(EvaluationStrategy eval, TupleExpr expr) {
		try {
			return asList(eval.evaluate(expr, EmptyBindingSet.getInstance()));
		} catch (QueryEvaluationException e) {  // Sesame 3: StoreException
			LOGGER.error("failed to evaluate query: " + expr, e);
		}
		return null;
	}
	
	/**
	 * Evaluates a given SPARQL query on the specified sail.
	 *  
	 * @param sail the Sail to use for query evaluation.
	 * @param query the SPARQL query to evaluate.
	 * @param includeInferred indicates the inclusion of inferred triples.
	 * @return the list of result bindings.
	 */
	public static List<BindingSet> eval(Sail sail, String query, boolean includeInferred) {
		if (sail == null)
			throw new IllegalArgumentException("sail must not be null");
		
		try {
			SailConnection con = sail.getConnection();
			
			try {
//				QueryModel model = parser.parseQuery(query, null);
//				return asList(con.evaluate(model, EmptyBindingSet.getInstance(), includeInferred));
//				SESAME 2:
				ParsedQuery model = parser.parseQuery(query, null);
				return asList(con.evaluate(model.getTupleExpr(), model.getDataset(), EmptyBindingSet.getInstance(), includeInferred));
			} catch (MalformedQueryException e) {
				LOGGER.error("Malformed query:\n" + query, e.getMessage());
				throw new IllegalArgumentException("Malformed query: " + e.getMessage());
			} catch (SailException e) {  // Sesame 3: StoreException
				LOGGER.error("failed to evaluate query: " + query, e);
			} finally {
				con.close();
			}
		} catch (SailException e) {  // Sesame 3: StoreException
			LOGGER.error("failed to open/close sail connection", e);
		}
		return null;
	}
	
	/**
	 * Evaluates a given SPARQL query on the specified repository.
	 *  
	 * @param rep the repository to use for query evaluation.
	 * @param query the SPARQL query to evaluate.
	 * @return the list of query results.
	 */
	public static List<BindingSet> eval(Repository rep, String query) {
		if (rep == null)
			throw new IllegalArgumentException("repository must not be null");
		
		try {
			RepositoryConnection con = rep.getConnection();
			
			try {
				TupleQuery tupleQuery = con.prepareTupleQuery(SPARQL, query);
				return asList(wrapResult(tupleQuery, rep.toString()));
			} catch (IllegalArgumentException e) {
				LOGGER.error("not a tuple query:\n" + query, e);
			} catch (MalformedQueryException e) {
				LOGGER.error("malformed query:\n" + query, e);
				throw new IllegalArgumentException("Malformed query: ", e);
			} catch (UnsupportedQueryLanguageException e) {
				LOGGER.error("repository does not support SPARQL: " + rep, e);
			} catch (RepositoryException e) {  // Sesame 3: StoreException
				LOGGER.error("failed to evaluate query on repository: " + rep + "\n" + query, e);
				Throwable cause = e.getCause();
				for (int i = 1; cause != null; cause = cause.getCause(), i++) {
					LOGGER.error("CAUSE " + i + ": " + cause);
				}
			} finally {
				// cannot close the connection if the Tuple result is still used
				// need to return a tuple result which closes the connection
				con.close();
			}
		} catch (RepositoryException e) {  // Sesame 3: StoreException
			LOGGER.error("failed to open/close repository connection", e);
		}
		return null;
	}
	
	/**
	 * Evaluates a given SPARQL query on the specified SPARQL endpoint.
	 * 
	 * @param endpoint the SPARQL endpoint to use for query evaluation.
	 * @param query the query to evaluate.
	 * @return the result.
	 */
//	public static Cursor<BindingSet> eval(String endpoint, String query) {
	public static CloseableIteration<BindingSet, QueryEvaluationException> eval(String endpoint, String query, BindingSet bindings) {
		try {
			return wrapResult(prepareTupleQuery(query, endpoint, bindings), endpoint);
		} catch (MalformedQueryException e) {
			LOGGER.error("Malformed query:\n" + query, e.getMessage());
			throw new IllegalArgumentException("Malformed query:\n" + query, e);
//		} catch (StoreException e) {
		} catch (RepositoryException e) {
			System.out.println("ARGGGHH");
			LOGGER.error("failed to evaluate query on endpoint: " + endpoint + "\n" + query, e);
//			return EmptyCursor.getInstance();
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		}
	}
	
	/**
	 * Prepares a TupleQuery for a SPARQL endpoint.
	 */
	public static TupleQuery prepareTupleQuery(String query, String endpoint, BindingSet bindings)
			throws RepositoryException, MalformedQueryException {  // SESAME 2:
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("query endpoint " + endpoint + ": '" + query.replace("\n", " ") + "'");
			if (bindings != null  && bindings.size() > 0)
				LOGGER.debug("with bindings: " + bindings);
		}
		
		try {
			SPARQLRepository http = httpMap.get(endpoint);
			if (http == null) {
				http = new SPARQLRepository(endpoint);
				httpMap.put(endpoint, http);
			}
			
			return http.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query, null);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		
//		SPARQLConnection con = new SPARQLConnection(repository, url, subjects)
//		
//		HTTPRepository http = httpMap.get(endpoint);
//		if (http == null) {
//			http = new HTTPRepository(endpoint);
//			httpMap.put(endpoint, http);
//		}
//		return http.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query, null);
		
//			throws StoreException, MalformedQueryException {  // SESAME 3:
//		HTTPConnectionPool http = httpMap.get(endpoint);
//		if (http == null) {
//			http = new HTTPConnectionPool(endpoint);
//			httpMap.put(endpoint, http);
//		}
//		try {
//			query = URLEncoder.encode(query, "UTF-8");
//			http = http.location(http.getURL() + "?query=" + query);
//			return new HTTPTupleQuery(query, new TupleQueryClient(http));
////			return getHTTPTupleQuery(http, endpoint, query);
//		} catch (UnsupportedEncodingException e) {
//			throw new RuntimeException("URL encoding failed", e);
//		}
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Puts the bindings of the supplied result cursor into a list.
	 * The binding names are ignored if the cursor is already a tuple result. 
	 * 
	 * @param cursor the cursor containing the result bindings.
	 * @return the list of result bindings.
	 * @throws StoreException if a problem occurs during result retrieval.
	 */
//	private static List<BindingSet> asList(Cursor<? extends BindingSet> cursor) {
	private static List<BindingSet> asList(CloseableIteration<? extends BindingSet, QueryEvaluationException> cursor) {
		if (cursor == null)
			return null;
		
		try {
			List<BindingSet> bindings = new ArrayList<BindingSet>();
			// Sesame 3:
//			BindingSet next = null;
//			while ((next = cursor.next()) != null)
//				bindings.add(next);
			// Sesame 2:
			while (cursor.hasNext())
				bindings.add(cursor.next());
			return bindings;
		} catch (QueryEvaluationException e) {  // Sesame 3: StoreException
			LOGGER.error("asList() error - CAUSE: " + e.getCause());
			LOGGER.error("failed to process query results", e);
		} finally {
			try {
				cursor.close();
			} catch (QueryEvaluationException e) {  // Sesame 3: StoreException
				LOGGER.warn("failed to close result cursor", e);
			}
		}
		return null;
	}
	
	/**
	 * Evaluates the supplied TupleQuery.
	 * 
	 * @tupleQuery the TupleQuery to evaluate.
	 * @target the target of the evaluation. (for debugging. TODO change to getDataset())
	 */
//	private static Cursor<BindingSet> wrapResult(TupleQuery tupleQuery, final String target) {
	private static CloseableIteration<BindingSet, QueryEvaluationException> wrapResult(final TupleQuery tupleQuery, final String target) {
		
		// Use result wrapper to catch (HTTP) communication errors.
		// next result will be null if an error occurs.
//		return new DelegatingCursor<BindingSet>(tupleQuery.evaluate()) {
		return new LookAheadIteration<BindingSet, QueryEvaluationException>() {

			private TupleQuery query = tupleQuery;
			private TupleQueryResult result;

			@Override
//			public BindingSet next() throws StoreException {
			public BindingSet getNextElement() {
				try {
//					return super.next();
					if (result == null)
						result = tupleQuery.evaluate();
					if (result.hasNext())
						return result.next();
					else
						return null;
				} catch (QueryEvaluationException e) {  // Sesame 3: StoreException
					// first check for network connection error
					Throwable cause = e.getCause();
					for (; cause != null; cause = cause.getCause()) {
						if (cause instanceof UnknownHostException) {
							LOGGER.error("cannot resolve endpoint " + target + ", " + cause);
							throw new RuntimeException("cannot resolve endpoint " + target, e);
						}
						if (cause instanceof ConnectException) {
							LOGGER.error("cannot connect to " + target + ", " + cause);
							throw new RuntimeException("cannot connect to " + target, e);
						}
						if (cause instanceof IOException) {
							LOGGER.error("problem with connection to " + target + ", " + cause);
							throw new RuntimeException("problem with cannot connect to " + target, e);
						}
					}
					LOGGER.error("cannot evaluate query on " + target + ", " + cause, e);
					throw new RuntimeException("cannot evaluate query on " + target, e);
//					return new EmptyBindingSet();
				}
			}
			//				@Override // Sesame 3:
			//				public void close() throws StoreException {
			//					try {
			//						super.close();
			//					} catch (StoreException e) {
			//						// no need to log the same exception twice
			//						if (cause == null || !cause.equals(e.getCause()))
			//							LOGGER.error("cannot close cursor for '" + target + "': " + e.getCause());
			//					}
			//				}
		};
	}

}
