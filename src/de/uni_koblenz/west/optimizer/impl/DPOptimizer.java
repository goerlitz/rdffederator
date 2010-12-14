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
package de.uni_koblenz.west.optimizer.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.Format;
import de.uni_koblenz.west.optimizer.Operator;
import de.uni_koblenz.west.optimizer.Optimizer;
import de.uni_koblenz.west.optimizer.QueryModel;
import de.uni_koblenz.west.optimizer.eval.CostCalculator;

/**
 * Join optimizer using dynamic programming.
 * 
 * Generates all possible join plans. Starts with the access plans and
 * iteratively combines all n-ary join plans. The generated plans are
 * pruned based on their cost.
 * 
 * @author Olaf Goerlitz
 * 
 * @param <O> the query operator type used in the query model.
 */
public class DPOptimizer<O extends Operator> implements Optimizer<O> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DPOptimizer.class);
	
	private static int DEBUG_N;

	private PlanCollection<O> optPlans;
	private CostCalculator<O> costEval;
	
	public DPOptimizer(CostCalculator<O> evaluator) {
		this.costEval = evaluator;
	}
	
	@Override
	public void optimize(QueryModel<O> model) {
		
		long time = System.currentTimeMillis();
		
		optPlans = new PlanCollection<O>();
		int count = model.getBaseRelationCount();
		
		// create access plans for all statement patterns
		Set<O> plans = model.createAccessPlans();
		prune(plans, model);
		optPlans.add(plans, 1);
		
		// create all n-ary join combinations [n = 2 .. #plans]
		for (int n = 2; n <= count; n++) {
			
			// store current n-ary join count for debugging
			DEBUG_N = n;
			
			// build n-ary joins by combining i-ary joins and (n-i)-ary joins
			for (int i = 1; i < n; i++) {
				
				Set<O> plans1 = optPlans.get(i);
				Set<O> plans2 = optPlans.get(n-i);
				
				// find for each plan all complementary plans to join
//				for (O plan : optPlans.get(i)) {
				for (O plan : plans1) {
//					Set<O> comp = model.filterDistinctPlans(plan, optPlans.get(n-i));
					Set<O> comp = model.filterDistinctPlans(plan, plans2);
					plans = model.createJoins(plan, comp);
					optPlans.add(plans, n);
				}
			}
			
			Set<O> nAryPlans = optPlans.get(n);
			
			// check for cross product
			if (nAryPlans.size() == 0) {
				LOGGER.debug(n + "-ary plans require cross product");
				// TODO: create cross products
			}
			
			prune(optPlans.get(n), model);
		}
		
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("time taken for optimization: " + (System.currentTimeMillis() - time));
		
		// return the final plan
		if (optPlans.get(count).size() == 0)
			throw new UnsupportedOperationException("Query requiring cross products are not supported (yet)");
		model.replaceRoot(optPlans.get(count).iterator().next());
	}
	
	/**
	 * Prune inferior plans - only keep plans with the lowest cost estimate.
	 * 
	 * Computes the cost for each plan and retains only the best plans.
	 * Since some plans are not comparable there can be several best plans.
	 */
	private void prune(Set<O> plans, QueryModel<O> model) {

		int eqClass = 0;
		Set<O> bestPlans = new HashSet<O>();
		Set<Set<O>> equivalentPlans = model.groupEquivalentPlans(plans);

		// DEBUG
		if (LOGGER.isDebugEnabled()) {
			String arity = DEBUG_N < 2 ? "access" : DEBUG_N + "-ary join";
			LOGGER.debug("pruning " + plans.size() + " " + arity + " plans in " + equivalentPlans.size() + " equivalence classes");
			if (LOGGER.isTraceEnabled()) {
				int eqs = 0;
				for (Set<O> equalSet : equivalentPlans) {
					eqs++;
					for (O plan : equalSet) {
						String pStr = plan.toString().replace("\n", " ");
						LOGGER.trace("SET [" + eqs + "] " + pStr);
					}
				}
			}
		}
		
		// Compare all plan from same equivalence class to find best one
		for (Set<O> equalSet : equivalentPlans) {
			
			int planCount = 0;
			eqClass++;

			O bestPlan = null;
			double lowestCost = Double.MAX_VALUE;
			
			for (O plan : equalSet) {
				
				planCount++;
				double cost = costEval.eval(plan);
				
				if (bestPlan == null || cost < lowestCost) {
					
					if (LOGGER.isTraceEnabled() && bestPlan != null) {
						LOGGER.trace("found better plan (" + Format.d(cost, 2) + " < " + Format.d(lowestCost, 2) + "): " + planCount + " of " + equalSet.size() + " in class " + eqClass);
					}
					
					bestPlan = plan;
					lowestCost = cost;
				} else {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("discarding plan (" + Format.d(cost, 2) + " > " + Format.d(lowestCost, 2) + "): " + planCount + " of " + equalSet.size() + " in class " + eqClass);
					}
				}
			}
			
			bestPlans.add(bestPlan);
		}
		
		plans.clear();
		plans.addAll(bestPlans);
		
		if (LOGGER.isTraceEnabled() && DEBUG_N > 1)
			LOGGER.trace("reduced to " + bestPlans.size() + " " + DEBUG_N + "-ary join plans.");
	}

}
