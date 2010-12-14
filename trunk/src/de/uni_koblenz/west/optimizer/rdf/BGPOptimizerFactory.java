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
package de.uni_koblenz.west.optimizer.rdf;

import de.uni_koblenz.west.optimizer.OptimizerFactory;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimator;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.rdf.eval.BGPCardinalityEstimator;
import de.uni_koblenz.west.optimizer.rdf.eval.BGPCostCalculator;
import de.uni_koblenz.west.optimizer.rdf.eval.SparqlExecutor;
import de.uni_koblenz.west.optimizer.rdf.eval.TrueCardinalityCounter;
import de.uni_koblenz.west.statistics.RDFStatistics;

public class BGPOptimizerFactory<P, F> extends OptimizerFactory<BGPOperator<P, F>> {
	
	protected RDFStatistics stats;
	protected ModelAdapter<P, F> adapter;
	protected SparqlExecutor executor;
	
	public BGPOptimizerFactory(ModelAdapter<P, F> adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("model adapter must not be NULL");
		this.adapter = adapter;
	}
	
	/**
	 * Set the statistics to use for optimizations.
	 * @param rdfStats the statistical data.
	 */
	public void setStatistics(RDFStatistics stats) {
		if (stats == null)
			throw new IllegalArgumentException("RDF statistics must not be NULL");
		this.stats = stats;
	}
	
	public void setSparqlExecutor(SparqlExecutor executor) {
		if (executor == null)
			throw new IllegalArgumentException("SPARQL executor must not be NULL");
		this.executor = executor;
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a cardinality estimator of the specified type.
	 * 
	 * @param type the cardinality estimation type.
	 * @return the cardinality estimator.
	 */
	@Override
	public CardinalityEstimator<BGPOperator<P, F>> getEstimator(CardinalityEstimatorType type) {
		
		CardinalityEstimator<BGPOperator<P, F>> estimator;
		
		switch (type) {
		case STATISTICS:
			if (stats == null)
				throw new UnsupportedOperationException("need statistics for estimator of type: " + type);
			estimator = new BGPCardinalityEstimator<P, F>(stats, adapter);
			break;
		case TRUE_COUNT:
			if (executor == null)
				throw new UnsupportedOperationException("need SPARQL evaluator for true cardinality estimation");
			estimator = new TrueCardinalityCounter<P, F>(executor, this.adapter);
			break;
		default:
			throw new IllegalArgumentException("unsupported estimator type: " + type);
		}
		
		return estimator;
	}
	
	/**
	 * Returns a cost calculator which uses the supplied cardinality estimator
	 * and cost model.
	 * 
	 * @param estimator the cardinality estimator to use.
	 * @param costModel the cost model to use.
	 * @return the cost calculator.
	 */
	@Override
	public BGPCostCalculator<P, F> getCostCalculator(CardinalityEstimator<BGPOperator<P, F>> estimator, CostModel costModel) {
		return new BGPCostCalculator<P, F>(estimator, costModel);
	}
	
	
}
