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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.algebra.helpers.VarNameCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.estimation.AbstractCostEstimator;
import de.uni_koblenz.west.federation.helpers.FilterConditionCollector;
import de.uni_koblenz.west.federation.helpers.Format;
import de.uni_koblenz.west.federation.model.BindJoin;
import de.uni_koblenz.west.federation.model.HashJoin;
import de.uni_koblenz.west.federation.model.SubQueryBuilder;
import de.uni_koblenz.west.federation.sources.SourceSelector;

/**
 * @author Olaf Goerlitz
 */
public class DynamicProgrammingOptimizer extends AbstractFederationOptimizer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicProgrammingOptimizer.class);
	
	private static int DEBUG_N;
	
	private boolean bindJoin;
	private boolean hashJoin;
			
	public DynamicProgrammingOptimizer(SourceSelector selector, SubQueryBuilder builder, AbstractCostEstimator estimator, boolean hashJoin, boolean bindJoin) {
		super(selector, builder, estimator);
		
		this.bindJoin = bindJoin;
		this.hashJoin = hashJoin;
	}

	@Override
	public void optimizeBGP(TupleExpr bgp) {
		
		long time = System.currentTimeMillis();

		PlanCollection optPlans = new PlanCollection();
		List<ValueExpr> conditions = FilterConditionCollector.process(bgp);

		// create access plans for all statement patterns
		List<TupleExpr> plans = this.getBaseExpressions(bgp);
		int count = plans.size();
		prune(plans);
		optPlans.add(new HashSet<TupleExpr>(plans), 1);
		
		// create all n-ary join combinations [n = 2 .. #plans]
		for (int n = 2; n <= count; n++) {
			
			// store current n-ary join count for debugging
			DEBUG_N = n;
			
			// build n-ary joins by combining i-ary joins and (n-i)-ary joins
			for (int i = 1; i < n; i++) {
				
				Set<TupleExpr> plans1 = optPlans.get(i);
				Set<TupleExpr> plans2 = optPlans.get(n-i);
				
				// find for each plan all complementary plans to join
				for (TupleExpr plan : plans1) {
					Set<TupleExpr> comp = filterDistinctPlans(plan, plans2);
					// we may not always find complementary plans
					if (comp.size() > 0) {
						plans = createJoins(plan, comp, conditions);
						optPlans.add(new HashSet<TupleExpr>(plans), n);
					}
				}
			}
			
			if (LOGGER.isTraceEnabled())
				LOGGER.trace(optPlans.get(n).size() + " plans generated for N=" + n);
			
			Set<TupleExpr> nAryPlans = optPlans.get(n);
			
			// check for cross product
			if (nAryPlans.size() == 0) {
				LOGGER.debug(n + "-ary plans require cross product");
				// TODO: create cross products
			}
			
			prune(optPlans.get(n));
		}
		
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("time taken for optimization: " + (System.currentTimeMillis() - time));
		
		// return the final plan
		if (optPlans.get(count).size() == 0)
			throw new UnsupportedOperationException("Queries requiring cross products are not supported (yet)");
		
		bgp.replaceWith(optPlans.get(count).iterator().next());

	}
	
	public List<TupleExpr> createJoins(TupleExpr joinPlan, Set<TupleExpr> plans, List<ValueExpr> conditions) {
		
		List<TupleExpr> newPlans = new ArrayList<TupleExpr>();
		
		for (TupleExpr plan : plans) {
			
			// TODO: avoid cross products as long as possible
			// keep only plans which have at least one join variable in common
			Set<String> vars = VarNameCollector.process(joinPlan);
			vars.retainAll(VarNameCollector.process(plan));
			if (vars.size() == 0) continue;
			
			// create all physical join variations
			for (TupleExpr join : createPhysicalJoins(joinPlan, plan)) {
				join = applyFilters(join, conditions);
				newPlans.add(join);
			}
		}
		if (newPlans.size() == 0)
			throw new IllegalStateException("no physical joins created. please enable them: " + joinPlan + " -> " + plans);
		
		return newPlans;
	}
	
	private List<Join> createPhysicalJoins(TupleExpr leftArg, TupleExpr rightArg) {
		List<Join> joins = new ArrayList<Join>();
		if (bindJoin) {
			joins.add(new BindJoin(leftArg, rightArg));
		}
		if (hashJoin) {
			joins.add(new HashJoin(leftArg, rightArg));
		}
		return joins;
	}
	
	protected TupleExpr applyFilters(TupleExpr operator, List<ValueExpr> conditions) {
		
		// copy given filters and remove all which are already applied
		Set<ValueExpr> newFilters = new HashSet<ValueExpr>(conditions);
		newFilters.removeAll(FilterConditionCollector.process(operator));
		
		
		// check if node satisfies all variables for remaining filters
		Set<String> nodeVars = VarNameCollector.process(operator);
		Iterator<ValueExpr> it = newFilters.iterator();
		while (it.hasNext()) {
			ValueExpr filter = it.next();
			if (!nodeVars.containsAll(VarNameCollector.process(filter)))
				it.remove();
		}
		
		// apply filters
		for (ValueExpr condition : newFilters) {
			operator = new Filter(operator, condition);
		}
		
		return operator;
	}
	
	public Set<Set<TupleExpr>> groupEquivalentPlans(Collection<TupleExpr> plans) {
		
		// put query plans in same category if the have the same patterns
		Map<Set<StatementPattern>, Set<TupleExpr>> equivalent = new HashMap<Set<StatementPattern>, Set<TupleExpr>>();

		// Collect all equivalent plans, i.e. plans with the same patterns
		for (TupleExpr plan : plans) {
			Set<StatementPattern> patterns = new HashSet<StatementPattern>(StatementPatternCollector.process(plan));
			Set<TupleExpr> planSet = equivalent.get(patterns);
			
			if (planSet == null) {
				planSet = new HashSet<TupleExpr>();
				equivalent.put(patterns, planSet);
			}
			
			planSet.add(plan);
		}
		return new HashSet<Set<TupleExpr>>(equivalent.values());
	}
	
	public Set<TupleExpr> filterDistinctPlans(TupleExpr plan, Set<TupleExpr> plans) {
		
		// distinct plans don't share any triple patterns (intersection is empty)
		
		Collection<StatementPattern> planpatterns = StatementPatternCollector.process(plan);
		
		Set<TupleExpr> planSet = new HashSet<TupleExpr>();
		for (TupleExpr other : plans) {
			Collection<StatementPattern> patterns = StatementPatternCollector.process(other);
			patterns.retainAll(planpatterns);
			if (patterns.size() == 0)
				planSet.add(other);
		}
		return planSet;
	}
	
	/**
	 * Prune inferior plans - only keep plans with the lowest cost estimate.
	 * 
	 * Computes the cost for each plan and retains only the best plans.
	 * Since some plans are not comparable there can be several best plans.
	 */
	private void prune(Collection<TupleExpr> plans) {

		int eqClass = 0;
		Set<TupleExpr> bestPlans = new HashSet<TupleExpr>();
		Set<Set<TupleExpr>> equivalentPlans = groupEquivalentPlans(plans);

//		// DEBUG
//		if (LOGGER.isDebugEnabled()) {
//			String arity = DEBUG_N < 2 ? "access" : DEBUG_N + "-ary join";
//			LOGGER.debug("pruning " + plans.size() + " " + arity + " plans in " + equivalentPlans.size() + " equivalence classes");
//			if (LOGGER.isTraceEnabled()) {
//				int eqs = 0;
//				for (Set<O> equalSet : equivalentPlans) {
//					eqs++;
//					for (O plan : equalSet) {
//						String pStr = plan.toString().replace("\n", " ");
//						LOGGER.trace("SET [" + eqs + "] " + pStr);
//					}
//				}
//			}
//		}
		
		// Compare all plan from same equivalence class to find best one
		for (Set<TupleExpr> equalSet : equivalentPlans) {
			
			int planCount = 0;
			eqClass++;

			TupleExpr bestPlan = null;
			double lowestCost = Double.MAX_VALUE;
			
			for (TupleExpr plan : equalSet) {
				
				planCount++;
				double cost = costEstimator.process(plan);
				
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
		
//		if (LOGGER.isTraceEnabled() && DEBUG_N > 1)
//			LOGGER.trace("reduced to " + bestPlans.size() + " " + DEBUG_N + "-ary join plans.");
	}
	
	class PlanCollection {
		
		private Map<Integer, Set<TupleExpr>> planMap = new HashMap<Integer, Set<TupleExpr>>();
		
		/**
		 * Adds the collection of query plans to the set of n-ary joins.
		 * @param plans the plans to add.
		 * @param n the number of n-ary joins.
		 */
		public void add(Set<TupleExpr> plans, int n) {
			Set<TupleExpr> planSet = planMap.get(n);
			if (planSet == null)
				planMap.put(n, planSet = new HashSet<TupleExpr>());
			planSet.addAll(plans);
		}
		
		/**
		 * Return the collection of query plans for n-ary joins.
		 * @param n the number of n-ary joins.
		 * @return the collection of query plans.
		 */
		public Set<TupleExpr> get(int n) {
			return this.planMap.get(n);
		}
	}

}
