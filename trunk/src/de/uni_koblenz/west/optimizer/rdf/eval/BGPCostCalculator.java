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
package de.uni_koblenz.west.optimizer.rdf.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.optimizer.eval.CardinalityEstimator;
import de.uni_koblenz.west.optimizer.eval.CostCalculator;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;

/**
 * Cost evaluator for the Basic Graph Pattern model.
 * Uses a cardinality estimator and a cost model.
 * 
 * @author Olaf Goerlitz
 *
 * @param <P> the triple pattern type.
 * @param <F> the filter type.
 */
public class BGPCostCalculator<P, F> implements BGPModelEvaluator<P, F, Double>, CostCalculator<BGPOperator<P, F>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BGPCostCalculator.class);
	
	private CardinalityEstimator<BGPOperator<P, F>> cardEval;
	private CostModel costModel;
	private double cost;
	
	public BGPCostCalculator(CardinalityEstimator<BGPOperator<P, F>> cardEval, CostModel costModel) {
		this.cardEval = cardEval;
		this.costModel = costModel;
	}
	
	protected double computeCost(AccessPlan<P, F> plan) {
		return costModel.getEvalCost(cardEval.eval(plan).doubleValue());
	}
	
	protected double computeCost(BGPJoin<P, F> join) {
		Number leftCard = cardEval.eval(join.getLeft());
		Number rightCard = cardEval.eval(join.getRight());
		
//		LOGGER.debug("JoinCard: " + leftCard + " x " + rightCard + " (" + join.getLeft() + " x " + join.getRight());
		
		return costModel.getJoinCost(leftCard.doubleValue(), rightCard.doubleValue());
	}
	
	@Override
	public Double eval(BGPOperator<P, F> operator) {
		synchronized(this) {
			this.cost = 0;
			operator.accept(this);
			return this.cost;
		}
	}

	@Override
	public void visit(AccessPlan<P, F> plan) {
		double plancost = computeCost(plan);
		this.cost += plancost;
	}

	@Override
	public void visit(BGPJoin<P, F> join) {
		join.getLeft().accept(this);
		join.getRight().accept(this);
		double joincost = computeCost(join);
		this.cost += joincost;
		
//		LOGGER.debug("JoinCost: " + joincost + " (" + this.cost + ") " + join);
	}

}
