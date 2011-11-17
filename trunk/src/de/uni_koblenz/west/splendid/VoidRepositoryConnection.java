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
package de.uni_koblenz.west.splendid;

//import org.openrdf.model.LiteralFactory;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
//import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
//import org.openrdf.model.impl.BNodeFactoryImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.UnsupportedQueryLanguageException;
//import org.openrdf.result.ContextResult;
//import org.openrdf.result.ModelResult;
//import org.openrdf.result.NamespaceResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
//import org.openrdf.store.StoreException;

import de.uni_koblenz.west.splendid.helpers.QueryExecutor;
import de.uni_koblenz.west.splendid.helpers.ReadOnlyRepositoryConnection;

/**
 * RepositoryConnection that communicates with a SPARQL endpoint via HTTP.
 * 
 * @author Olaf Goerlitz
 */
public class VoidRepositoryConnection extends ReadOnlyRepositoryConnection {
	
	protected final String endpoint;
	protected final ValueFactory vf;
	
	/**
	 * Creates a RepositoryConnection for the voiD repository.
	 * 
	 * @param repository the repository which is connected.
	 */
	public VoidRepositoryConnection(VoidRepository repository) {
		super(repository);
		
		this.endpoint = repository.getEndpoint().stringValue();
		
		// reuse repository specific factories for better performance
//		BNodeFactoryImpl bf = new BNodeFactoryImpl();
//		URIFactory uf = repository.getURIFactory();
//		LiteralFactory lf = repository.getLiteralFactory();
//		this.vf = new ValueFactoryImpl(bf, uf, lf);
		this.vf = new ValueFactoryImpl();
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
//			throws StoreException, MalformedQueryException {
			throws RepositoryException, MalformedQueryException {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
//			throws StoreException, MalformedQueryException {
			throws RepositoryException, MalformedQueryException {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
//			throws StoreException, MalformedQueryException {
			throws RepositoryException, MalformedQueryException {
		throw new UnsupportedOperationException("not yet implemented");
	}

	/**
	 * Prepares a query that produces sets of value tuples.
	 * 
	 * @param ql
	 *        The query language in which the query is formulated.
	 * @param query
	 *        The query string.
	 * @param baseURI
	 *        The base URI to resolve any relative URIs that are in the query
	 *        against, can be <tt>null</tt> if the query does not contain any
	 *        relative URIs.
	 * @throws IllegalArgumentException
	 *         If the supplied query is not a tuple query.
	 * @throws MalformedQueryException
	 *         If the supplied query is malformed.
	 * @throws UnsupportedQueryLanguageException
	 *         If the supplied query language is not supported.
	 */
	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
//			throws StoreException, MalformedQueryException {
			throws RepositoryException, MalformedQueryException {
		
		if (ql != QueryLanguage.SPARQL)
			throw new UnsupportedQueryLanguageException("only SPARQL supported");
		if (query == null)
			throw new IllegalArgumentException("query is null");
		if (baseURI != null)
			throw new IllegalArgumentException("base/relative URIs not allowed");
		
		return QueryExecutor.prepareTupleQuery(query, this.endpoint, null);
	}

	@Override
//	public <H extends RDFHandler> H exportMatch(Resource subj, URI pred, Value obj, boolean includeInferred, H handler, Resource... contexts) throws StoreException, RDFHandlerException {
	public void exportStatements(Resource subj, URI pred, Value obj, boolean includeInferred, RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	@Override
//	public ContextResult getContextIDs() throws StoreException {
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
//	public String getNamespace(String prefix) throws StoreException {
	public String getNamespace(String prefix) throws RepositoryException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
//	public NamespaceResult getNamespaces() throws StoreException {
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	@Override
//	public ModelResult match(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws StoreException {
	public RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	// Sesame 2 only ===========================================================
	
	@Override
	public long size(Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	// Sesame 3 only ===========================================================
	
	/**
	 * Gets a ValueFactory for this RepositoryConnection.
	 * 
	 * @return A repository-specific ValueFactory.
	 */
	@Override
	public ValueFactory getValueFactory() {
		return this.vf;
	}
	
//	@Override
//	public void begin() throws StoreException {
//		throw new UnsupportedOperationException("not yet implemented");
//	}
//
//	@Override
//	public void close() throws StoreException {
//		throw new UnsupportedOperationException("not yet implemented");
//	}
//
//	@Override
//	public boolean isOpen() throws StoreException {
//		throw new UnsupportedOperationException("not yet implemented");
//	}
//
//	@Override
//	public ModelResult match(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts)
//			throws StoreException {
//		throw new UnsupportedOperationException("not yet implemented");
//	}
//
//	@Override
//	public long sizeMatch(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts)
//			throws StoreException {
//		throw new UnsupportedOperationException("not yet implemented");
//	}

}
