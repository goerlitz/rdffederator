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
package de.uni_koblenz.west.federation;

//import org.openrdf.cursor.Cursor;
//import org.openrdf.model.LiteralFactory;
import info.aduna.iteration.CloseableIteration;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
//import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
//import org.openrdf.model.impl.BNodeFactoryImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
//import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.impl.BindingAssigner;
import org.openrdf.query.algebra.evaluation.impl.CompareOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.openrdf.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.openrdf.query.algebra.evaluation.impl.FilterOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryModelPruner;
import org.openrdf.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.openrdf.query.algebra.evaluation.util.QueryOptimizerList;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.evaluation.FederationEvalStrategy;
import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.helpers.ReadOnlySailConnection;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;

/**
 * Wraps multiple remote repositories with SPARQL endpoints into one
 * {@link SailConnection}. Queries are split into fragments and forwarded to
 * endpoints which can probably return results.<br/><br/>
 * 
 * Statistics are used to select suitable remote repositories.
 * {@link TripleSource}s, which are used to evaluate single
 * statement patterns, are not employed.
 * 
 * @author Olaf Goerlitz
 */
public class FederationSailConnection extends ReadOnlySailConnection {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FederationSailConnection.class);

	private final SourceFinder<StatementPattern> finder;
//	private final RDFStatistics stats;
	private final ValueFactory vf;
	private final QueryOptimizer optimizer;
	
	/**
	 * Create a Sail connection which wraps the members repository connections.
	 * Adopted from <tt>FederationConnection.FederationConnection()</tt>.
	 * 
	 * @param sail the federation Sail.
	 * @param stats the statistics to use.
	 */
	public FederationSailConnection(FederationSail sail) {
		
		super(sail);  // mandatory for Sesame 2, obsolete in Sesame 3
		
		if (sail == null)
			throw new IllegalArgumentException("sail must not be NULL");
		
		this.optimizer = sail.getFederationOptimizer();
		this.finder = sail.getSourceFinder();

//		URIFactory uf = sail.getURIFactory();
//		LiteralFactory lf = sail.getLiteralFactory();
//		BNodeFactoryImpl bf = new BNodeFactoryImpl();
//		this.vf = new ValueFactoryImpl(bf, uf, lf);
		this.vf = new ValueFactoryImpl();
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Evaluates the supplied QueryModel on the federation members.
	 * 
	 * @param query
	 *            The query to evaluate.
	 * @param bindings
	 *            A set of input parameters for the query evaluation. The keys
	 *            reference variable names that should be bound to the value
	 *            they map to.
	 * @param includeInferred
	 *            Indicates whether inferred triples are to be considered in the
	 *            query result. If false, no inferred statements are returned;
	 *            if true, inferred statements are returned if available
	 * @return The result Cursor.
	 * @throws StoreException
	 *             If the Sail encountered an error or invalid internal state.
	 */
	@Override
//	public Cursor<? extends BindingSet> evaluate(QueryModel query,
//			BindingSet bindings, boolean includeInferred) throws StoreException {	// Sesame 3:
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr query, Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {	// Sesame 2:
		
		FederationEvalStrategy strategy = new FederationEvalStrategy(this.finder, this.vf);
		QueryOptimizerList optimizerList = new QueryOptimizerList();
		
		LOGGER.trace("Incoming query model:\n{}", OperatorTreePrinter.print(query));
		
		// Clone the tuple expression to allow for more aggressive optimizations
		query = query.clone();

		optimizerList.add(new BindingAssigner());
		optimizerList.add(new CompareOptimizer());
		optimizerList.add(new ConjunctiveConstraintSplitter());
		optimizerList.add(new DisjunctiveConstraintOptimizer());
		optimizerList.add(new SameTermFilterOptimizer());
//		optimizerList.add(new FilterOptimizer());
//		optimizerList.add(new QueryModelPruner());
		optimizerList.add(this.optimizer);

//		optimizerList.optimize(query, bindings);  // Sesame 3
		optimizerList.optimize(query, dataset, bindings);  // Sesame 2
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Optimized query model:\n{}", OperatorTreePrinter.print(query));
		
		try {
			return strategy.evaluate(query, EmptyBindingSet.getInstance());
		} catch (QueryEvaluationException e) {  // Sesame 3: StoreException
			throw new SailException("query evaluation failed", e);
		}
	}
	
	// Sesame 2 only ===========================================================
	
	@Override
//	public void close() throws StoreException {
	protected void closeInternal() throws SailException {
		// do nothing, calling super.close() creates a loop in Sesame 2
//		super.close(); // Sesame 3 only
	}

	@Override
//	public Cursor<? extends Resource> getContextIDs() throws StoreException {
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
//	public String getNamespace(String prefix) throws StoreException {
	protected String getNamespaceInternal(String prefix) throws SailException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
//	public Cursor<? extends Namespace> getNamespaces() throws StoreException {
	protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
//	public Cursor<? extends Statement> getStatements(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws StoreException {
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
//	public long size(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws StoreException {
	protected long sizeInternal(Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	// Sesame 3 only ===========================================================
	
//	/**
//	 * Gets a ValueFactory object that can be used to create URI-, blank node-,
//	 * literal- and statement objects.
//	 * 
//	 * @return a ValueFactory object for this Sail object.
//	 */
//	@Override
//	public ValueFactory getValueFactory() {
//		return this.vf;
//	}
	
}
