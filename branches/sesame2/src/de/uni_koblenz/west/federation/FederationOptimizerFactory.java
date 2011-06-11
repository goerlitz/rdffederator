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

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.ValueExpr;

import de.uni_koblenz.west.federation.adapter.SesameAdapter;
import de.uni_koblenz.west.federation.adapter.SesameSparqlExecutor;
import de.uni_koblenz.west.federation.sources.SourceSelector;
import de.uni_koblenz.west.optimizer.Optimizer;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.BGPOptimizerFactory;

/**
 * Creates optimizer based on the supplied settings.
 * 
 * @author Olaf Goerlitz
 */
public class FederationOptimizerFactory extends BGPOptimizerFactory<StatementPattern, ValueExpr> {
	
	private CostModel costModel;
	private SourceSelector finder;
	
	/**
	 * Creates a Sesame optimizer factory.
	 */
	public FederationOptimizerFactory() {
		super(new SesameAdapter());
		setSparqlExecutor(new SesameSparqlExecutor());
	}
	
	/**
	 * Sets the cost model for all consecutively generated optimizers which
	 * apply cost estimation.
	 * 
	 * @param costModel the cost model to use.
	 */
	public void setCostmodel(CostModel costModel) {
		this.costModel = costModel;
	}
	
	public void setSourceSelector(SourceSelector finder) {
		this.finder = finder;
	}
	
	/**
	 * Returns an optimizer with the specified characteristics.
	 * 
	 * @param optimizerType the type of the optimizer.
	 * @param estimatorType the type of the cardinality estimator.
	 * @param costModel the costModel to use.
	 * @return the optimizer.
	 */
	public FederationOptimizer getOptimizer(String optimizerType, String estimatorType) {
		Optimizer<BGPOperator<StatementPattern, ValueExpr>> optimizer = null;
		if ("DYNAMIC_PROGRAMMING".equals(optimizerType))
			optimizer = newOptimizer(getEstimator(estimatorType), this.costModel);
		if ("PATTERN_HEURISTIC".equalsIgnoreCase(optimizerType))
			optimizer = newOptimizer(getEstimator(estimatorType));
		
		if (optimizer != null)
			return new FederationOptimizer(optimizer, this.finder);
		
		throw new IllegalArgumentException("unknown optimizer type: " + optimizerType);
	}
	
}
