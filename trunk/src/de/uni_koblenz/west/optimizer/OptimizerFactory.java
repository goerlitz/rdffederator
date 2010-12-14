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
package de.uni_koblenz.west.optimizer;

import de.uni_koblenz.west.optimizer.eval.CardinalityEstimator;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType;
import de.uni_koblenz.west.optimizer.eval.CostCalculator;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.impl.DPOptimizer;
import de.uni_koblenz.west.optimizer.impl.PatternOrderOptimizer;

/**
 * Base factory for creating an {@link Optimizer}.
 * 
 * @author Olaf Goerlitz
 *
 * @param <O> the query operator type used in the query model.
 */
public abstract class OptimizerFactory<O extends Operator> {
	
	/**
	 * Creates a heuristics-based optimizer using the given cardinality
	 * estimator.
	 * 
	 * @param estimator the cardinality estimator to use.
	 * @return the optimizer.
	 */
	public Optimizer<O> newOptimizer(CardinalityEstimator<O> estimator) {
		return new PatternOrderOptimizer<O>(estimator);
	}

	/**
	 * Creates a dynamic programming based optimizer using the cardinality
	 * estimator and cost Model.
	 * 
	 * @param estimator the cardinality estimator to use.
	 * @param costModel the cost model to use.
	 * @return the optimizer.
	 */
	public Optimizer<O> newOptimizer(CardinalityEstimator<O> estimator, CostModel costModel) {
		return new DPOptimizer<O>(getCostCalculator(estimator, costModel));
	}
	
	public CostCalculator<O> getCostCalculator(CardinalityEstimatorType type, CostModel costModel) {
		return getCostCalculator(getEstimator(type), costModel);
	}
	
	// ABSTRACT ----------------------------------------------------------------
	
	/**
	 * Returns a cardinality estimator of the specified type.
	 * 
	 * @param type the cardinality estimation type.
	 * @return the cardinality estimator.
	 */
	public abstract CardinalityEstimator<O> getEstimator(CardinalityEstimatorType type);
	
	/**
	 * Returns a cost calculator which uses the supplied cardinality estimator
	 * and cost model.
	 * 
	 * @param estimator the cardinality estimator to use.
	 * @param costModel the cost model to use.
	 * @return the cost calculator.
	 */
	public abstract CostCalculator<O> getCostCalculator(CardinalityEstimator<O> estimator, CostModel costModel);
	
	// -------------------------------------------------------------------------
	
	protected CardinalityEstimator<O> getEstimator(String estimatorName) {
		try {
			return getEstimator(CardinalityEstimatorType.valueOf(estimatorName));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("unsupported cardinality estimator: " + estimatorName);
		}
	}
	
}
