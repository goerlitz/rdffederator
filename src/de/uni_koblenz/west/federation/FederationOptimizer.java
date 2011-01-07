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

import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
//import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.adapter.SesameAdapter;
import de.uni_koblenz.west.federation.adapter.SesameBGPWrapper;
import de.uni_koblenz.west.federation.helpers.BasicGraphPatternCollector;
import de.uni_koblenz.west.optimizer.Optimizer;
import de.uni_koblenz.west.optimizer.eval.QueryModelEvaluator;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;
import de.uni_koblenz.west.optimizer.rdf.eval.QueryModelVerifier;
import de.uni_koblenz.west.optimizer.rdf.util.BGPModelPrinter;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * A Sesame specific optimizer for federated query optimization.
 * 
 * @author Olaf Goerlitz
 */
public class FederationOptimizer implements QueryOptimizer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FederationOptimizer.class);
	
	protected Optimizer<BGPOperator<StatementPattern, ValueExpr>> optimizer;
	protected SourceFinder<StatementPattern> finder;
	
	protected QueryModelVerifier<StatementPattern, ValueExpr> listener;
	protected BGPModelPrinter<StatementPattern, ValueExpr> printer = new BGPModelPrinter<StatementPattern, ValueExpr>(new SesameAdapter());
	
	/**
	 * Creates a federation optimizer based on the supplied generic optimizer and source finder.
	 * 
	 * @param optimizer the generic query optimizer to use.
	 * @param finder the source finder to use.
	 */
	protected FederationOptimizer(Optimizer<BGPOperator<StatementPattern, ValueExpr>> optimizer, SourceFinder<StatementPattern> finder) {
		if (optimizer == null)
			throw new IllegalArgumentException("the optimizer must not be null");
		if (finder == null)
			throw new IllegalArgumentException("source finder must not be null");

		this.optimizer = optimizer;
		this.finder = finder;
	}

	/**
	 * Extract and optimize all basic graph patterns.
	 */
	@Override
//	public void optimize(QueryModel query, BindingSet bindings) throws StoreException {  // Sesame 3
	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {  // Sesame 2
		
		List<SesameBGPWrapper> bgps = new BasicGraphPatternCollector().eval(query);
		if (bgps.size() == 0)
			LOGGER.debug("found no basic graph patterns to optimize");
		
		// optimize every basic graph pattern
		for (SesameBGPWrapper bgp : bgps) {
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("--- ORIGINAL BGP ---\n{}", bgp);
			}

			bgp.setSourceFinder(finder);
			optimizer.optimize(bgp);
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("--- OPTIMIZED BGP ---\n{}", bgp);
			}
			
//			if (this.listener != null) {
//				this.listener.resultObtained(listener.getEvaluator().eval(bgp.getRoot()));
//			}
		}
	}
	
	public void setResultVerifier(QueryModelVerifier<StatementPattern, ValueExpr> verifier) {
		this.listener = verifier;
	}
	
	public void setEvaluator(QueryModelEvaluator<BGPOperator<StatementPattern, ValueExpr>, ? extends Number> evaluator) {
		this.printer.setEvaluator(evaluator);
	}
}
