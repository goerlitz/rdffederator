/*
 * This file is part of RDF Federator.
 * Copyright 2011 Olaf Goerlitz
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
package de.uni_koblenz.west.federation.optimizer;

import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.estimation.VoidCardinalityEstimator;
import de.uni_koblenz.west.federation.helpers.AnnotatingTreePrinter;
import de.uni_koblenz.west.federation.helpers.FilterConditionCollector;
import de.uni_koblenz.west.federation.model.BasicGraphPatternExtractor;
import de.uni_koblenz.west.federation.model.MappedStatementPattern;
import de.uni_koblenz.west.federation.model.SubQueryBuilder;
import de.uni_koblenz.west.federation.sources.SourceSelector;

/**
 * Base functionality for federated query optimizers
 * 
 * @author Olaf Goerlitz
 */
public abstract class AbstractFederationOptimizer implements QueryOptimizer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFederationOptimizer.class);
	
	protected SourceSelector selector;
	protected SubQueryBuilder builder;
	protected VoidCardinalityEstimator estimator;
	
	public AbstractFederationOptimizer(SourceSelector selector, SubQueryBuilder builder, VoidCardinalityEstimator estimator) {
		if (selector == null)
			throw new IllegalArgumentException("source selector must not be null");
		if (builder == null)
			throw new IllegalArgumentException("pattern group builder must not be null");
		if (estimator == null)
			throw new IllegalArgumentException("cardinality estimator must not be null");
		
		this.selector = selector;
		this.builder = builder;
		this.estimator = estimator;
	}
	
	public abstract void optimizeBGP(TupleExpr query);
	
	protected List<TupleExpr> getBaseExpressions(TupleExpr expr) {
		
		// get patterns and filter conditions from query model
		List<StatementPattern> patterns = StatementPatternCollector.process(expr);
		List<ValueExpr> conditions = FilterConditionCollector.process(expr);
		
		// create patterns with source mappings
		List<MappedStatementPattern> mappedPatterns = this.selector.mapSources(patterns);
		
		return this.builder.createSubQueries(mappedPatterns, conditions);
	}
	
	@Override
	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {  // Sesame 2
		
		// collect all basic graph patterns
		for (TupleExpr bgp : BasicGraphPatternExtractor.process(query)) {
			
//			// a single statement pattern needs no optimization
//			// TODO: need sources first
//			if (bgp instanceof StatementPattern)
//				continue;
			
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("BGP before optimization:\n" + AnnotatingTreePrinter.print(bgp));

			optimizeBGP(bgp);
			
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("BGP after optimization:\n" + AnnotatingTreePrinter.print(bgp.getParentNode(), estimator));
		}
		
	}
	
}
