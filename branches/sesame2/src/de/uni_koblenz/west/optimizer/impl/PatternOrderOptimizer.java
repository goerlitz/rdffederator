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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_koblenz.west.optimizer.Operator;
import de.uni_koblenz.west.optimizer.Optimizer;
import de.uni_koblenz.west.optimizer.QueryModel;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimator;

/**
 * Heuristics for ordering base relation based on their cardinality.
 * Starts with the triple pattern with the lowest selectivity. Adds 
 * more triple pattern in joins preferring low selectivity and triple
 * patterns with already bound variables. 
 * 
 * @author Olaf Goerlitz
 *
 * @param <O> the operator type.
 */
public class PatternOrderOptimizer<O extends Operator> implements Optimizer<O> {
	
	private CardinalityEstimator<O> estimator;
	
	public PatternOrderOptimizer(CardinalityEstimator<O> estimator) {
		this.estimator = estimator;
	}

	@Override
	public void optimize(QueryModel<O> query) {
		
		Set<O> plans = query.createAccessPlans();
		final Map<O, Double> costs = new HashMap<O, Double>();
		
		// get access plans
		for (O plan : plans) costs.put(plan, estimator.eval(plan).doubleValue());
		
		// sort plans ascending by cost
		List<O> planList = new ArrayList<O>(costs.keySet());
		Collections.sort(planList, new Comparator<O>() {

			@Override
			public int compare(O arg0, O arg1) {
				return costs.get(arg0).compareTo(costs.get(arg1));
			}
		});
		
		O join = null;
		Iterator<O> it = planList.iterator();
		while (!planList.isEmpty())
		while (it.hasNext()) {
			O plan = it.next();
			if (join == null) {
				join = plan;
				it.remove();
				plan = it.next();
			}
			
			Set<O> set = new HashSet<O>();
			set.add(plan);
			set = query.createJoins(join, set);
			if (!set.isEmpty()) {
				join = set.iterator().next();
				it.remove();
				it = planList.iterator();
			}
		}
		query.replaceRoot(join);
	}

}