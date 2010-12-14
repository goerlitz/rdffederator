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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.QueryModel;
import de.uni_koblenz.west.optimizer.rdf.util.BGPCollector;
import de.uni_koblenz.west.optimizer.rdf.util.BGPModelPrinter;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * Generic model for a Basic Graph Pattern tree structure.
 * The only elements are triple patterns, joins and filters.
 * Triple patterns and joins are tree nodes whereas filters are node attributes.
 * 
 * @author Olaf Goerlitz
 *
 * @param <P> the triple pattern type.
 * @param <F> the filter type.
 */
public abstract class BGPQueryModel<P, F> implements QueryModel<BGPOperator<P, F>> {
	
	protected static final boolean HANDLE_SAMEAS = true;

	protected List<BGPOperator<P, F>> baseOperators;
	protected ModelAdapter<P, F> adapter;
	protected RDFStatistics stats;
	
	public static enum JoinExec {
		DIRECT,
//		SEMI_JOIN
	}
	
	public static enum JoinAlgo {
		NESTED_LOOP,
//		SORT_MERGE
	}
	
	protected BGPQueryModel(ModelAdapter<P, F> adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("adapter must not be null");
		this.adapter = adapter;
	}
	
	protected AccessPlan<P, F> createPlan(P pattern, Set<Graph> sources) {
		return new AccessPlan<P, F>(pattern, sources);
	}
	
	protected BGPJoin<P, F> createJoin(JoinExec exec, JoinAlgo algo, BGPOperator<P, F> left, BGPOperator<P, F> right) {
		return new BGPJoin<P, F>(exec, algo, left, right);
	}
	
	// --- ABSTRACT ------------------------------------------------------------
	
	/**
	 * Returns all triple patterns of the model.
	 * @return the set of triple patterns.
	 */
	protected abstract Set<P> getAllPatterns();
	
	/**
	 * Returns all filters of the model.
	 * @return the set of filters.
	 */
	protected abstract Set<F> getAllFilters();
	
	// -------------------------------------------------------------------------
	
	private BGPOperator<P, F> joinPatterns(List<P> patterns, Set<Graph> sources) {
		// join order is not relevant - just avoid cross products
		
		P pattern = patterns.remove(0);
		BGPOperator<P, F> op = createPlan(pattern, sources);
		Set<String> joinVars = adapter.getPatternVars(pattern);
		
		while (patterns.size() > 0) {
			List<P> joinPatterns = extractJoinPatterns(joinVars, patterns);
			if (joinPatterns.size() == 0)
				throw new UnsupportedOperationException("cross product joins not supported");
			for (P joinPattern : joinPatterns) {
				op = createJoin(JoinExec.DIRECT, JoinAlgo.NESTED_LOOP, op, createPlan(joinPattern, sources));
				joinVars.addAll(adapter.getPatternVars(joinPattern));
			}
		}
		return op;
	}
	
	private List<P> extractJoinPatterns(Set<String> joinVars, List<P> patterns) {
		List<P> joinPatterns = new ArrayList<P>();
		Iterator<P> it = patterns.iterator();
		while (it.hasNext()) {
			P pattern = it.next();
			Set<String> vars = adapter.getPatternVars(pattern);
			vars.retainAll(joinVars);
			if (vars.size() > 0) {
				joinPatterns.add(pattern);
				it.remove();
			}
		}
		return joinPatterns;
	}
	
	private void createBaseRelations() {
		baseOperators = new ArrayList<BGPOperator<P,F>>();
		Map<Set<Graph>, List<P>> graphSets = findPlanSetsPerSource();
		
		// evaluation
		Set<Graph> graphs = new HashSet<Graph>();
		int planCount = 0;
		for (Set<Graph> graphSet : graphSets.keySet()) {
			graphs.addAll(graphSet);
//			if (graphSet.size() == 1) {
//				planCount++;
//			} else {
				planCount += graphSet.size() * graphSets.get(graphSet).size();
//			}
		}
		System.out.println(planCount + " plans for " + graphs.size() + " graphs: " + graphs);
		
		
		for (Set<Graph> graphSet : graphSets.keySet()) {
			List<P> plans = graphSets.get(graphSet);
			// combine all operators for one source
			if (graphSet.size() == 1) {
				baseOperators.add(joinPatterns(plans, graphSet));				
			} else {
				// or create separate operators for each source
				for (P pattern : plans) {
					baseOperators.add(createPlan(pattern, graphSet));
				}
			}
		}

//		for (BGPOperator<P, F> op : baseOperators)
//			System.out.println(new BGPModelPrinter<P, F>(adapter).eval(op));
	}
	
	private Map<Set<Graph>, List<P>> findPlanSetsPerSource() {
		Map<Set<Graph>, List<P>> graphSets = new HashMap<Set<Graph>, List<P>>();
		List<P> sameAsPatterns = new ArrayList<P>();
		
		// find sources for all patterns and create Map for {Graph}:{Pattern}
		for (P pattern : getAllPatterns()) {
			String[] values = adapter.getPatternConstants(pattern);
			
			// add sameAs patterns to extra list for special treatement
			if (HANDLE_SAMEAS && "http://www.w3.org/2002/07/owl#sameAs".equals(values[1])) {
				sameAsPatterns.add(pattern);
				continue;
			}
			
			// get sources for pattern and add pattern to the mapped list 
			Set<Graph> sources = stats.findSources(values[0], values[1], values[2]);
			List<P> patternList = graphSets.get(sources);
			if (patternList == null) {
				patternList = new ArrayList<P>();
				graphSets.put(sources, patternList);
			}
			patternList.add(pattern);
		}
		
		// special treatment of sameAs pattens
		if (HANDLE_SAMEAS) {
			
			Iterator<P> iterator = sameAsPatterns.iterator();
			while (iterator.hasNext()) {
				P sameAsPattern = iterator.next();
				String varName = adapter.getVarName(sameAsPattern, 0);
				List<Set<Graph>> targetSets = new ArrayList<Set<Graph>>();
				
				// look for graph sets which the same variable in their patterns
				for (Set<Graph> graphSet : graphSets.keySet()) {
					for (P pattern : graphSets.get(graphSet)) {
						if (adapter.getPatternVars(pattern).contains(varName)) {
							targetSets.add(graphSet);
						}
					}
				}
				
				// if there is only a single matching graph set, add the sameAs pattern
				// TODO Can we assign sameAs patterns to multiple graph sets?
				if (targetSets.size() == 1) {
					graphSets.get(targetSets.get(0)).add(sameAsPattern);
					iterator.remove();
				}
			}
			
			// find graph sets for unassigned sameAs patterns
			for (P sameAsPattern : sameAsPatterns) {
				String[] values = adapter.getPatternConstants(sameAsPattern);
				
				// get sources for pattern and add pattern to the mapped list 
				Set<Graph> sources = stats.findSources(values[0], values[1], values[2]);
				List<P> patternList = graphSets.get(sources);
				if (patternList == null) {
					patternList = new ArrayList<P>();
					graphSets.put(sources, patternList);
				}
				patternList.add(sameAsPattern);
			}
		}
		
//		// print pattern sets
//		for (Set<Graph> graphSet : graphSets.keySet()) {
//			System.out.println("BGPModel: Sources " + graphSet);
//			for (P pattern : graphSets.get(graphSet))
//				System.out.println("  " + adapter.toSparqlPattern(pattern));
//		}
		
		return graphSets;
	}
	
	// --- OVERRIDE ------------------------------------------------------------
	
	@Override
	public int getBaseRelationCount() {
		if (baseOperators == null)
			createBaseRelations();
		
		return baseOperators.size();
		
//		return getAllPatterns().size();
	}

	@Override
	public Set<BGPOperator<P, F>> createAccessPlans() {
		
		if (stats == null)
			throw new IllegalArgumentException("need statistics for pattern sources");
		
		if (baseOperators == null)
			createBaseRelations();
		
		return new HashSet<BGPOperator<P,F>>(baseOperators);
		
//		Set<F> filters = getAllFilters();
//		Set<BGPOperator<P, F>> plans = new HashSet<BGPOperator<P, F>>();
//		
//		for (P pattern : getAllPatterns()) {
//			
//			String[] values = adapter.getPatternConstants(pattern);
////			if (bound[1] == null)
////				throw new UnsupportedOperationException("unbound predicates not supported");
//			
//			// TODO: handle combinations of bound vars, e.g. pattern with type
////			try {
////				Set<Graph> sources = stats.findGraphs(new URI(bound[1]));
//				Set<Graph> sources = stats.findSources(values[0], values[1], values[2]);
//				AccessPlan<P, F> plan = createPlan(pattern, sources);
//				applyFilters(plan, filters);
//				plans.add(plan);
////			} catch (URISyntaxException e) {
////				// should not happen
////			}
//		}
//		return plans;
	}
	
	@Override
	public Set<BGPOperator<P, F>> createJoins(BGPOperator<P, F> joinPlan, Set<BGPOperator<P, F>> plans) {
		
		Set<F> filters = getAllFilters();
		Set<BGPOperator<P, F>> newPlans = new HashSet<BGPOperator<P, F>>();
		
		for (BGPOperator<P, F> plan : plans) {
			
			// TODO: avoid cross products as long as possible
			// keep only plans which have at least one join variable in common
			Set<String> vars = new VarsCollector().process(joinPlan);
			vars.retainAll(new VarsCollector().process(plan));
			if (vars.size() == 0) continue;
			
			for (JoinExec exec : JoinExec.values()) {
				for (JoinAlgo algo : JoinAlgo.values()) {
					BGPJoin<P, F> join = createJoin(exec, algo, joinPlan, plan);
					applyFilters(join, filters);
					newPlans.add(join);
				}
			}
		}
		return newPlans;
	}
	
	@Override
	public Set<Set<BGPOperator<P, F>>> groupEquivalentPlans(Set<BGPOperator<P, F>> plans) {
		
		Map<Set<P>, Set<BGPOperator<P, F>>> equivalent = new HashMap<Set<P>, Set<BGPOperator<P, F>>>();

		// Collect all equivalent plans, i.e. plans with the same patterns
		for (BGPOperator<P, F> plan : plans) {
			Set<P> patterns = BGPCollector.getPatterns(plan);
			Set<BGPOperator<P, F>> planSet = equivalent.get(patterns);
			
			if (planSet == null) {
				planSet = new HashSet<BGPOperator<P, F>>();
				equivalent.put(patterns, planSet);
			}
			
			planSet.add(plan);
		}
		return new HashSet<Set<BGPOperator<P, F>>>(equivalent.values());
	}
	
	@Override
	public Set<BGPOperator<P, F>> filterDistinctPlans(BGPOperator<P, F> plan, Set<BGPOperator<P, F>> plans) {
		
		// distinct plans don't share any triple patterns (intersection is empty)
		
		Set<P> planpatterns = BGPCollector.getPatterns(plan);
		
		Set<BGPOperator<P, F>> planSet = new HashSet<BGPOperator<P, F>>();
		for (BGPOperator<P, F> other : plans) {
			Set<P> patterns = BGPCollector.getPatterns(other);
			patterns.retainAll(planpatterns);
			if (patterns.size() == 0)
				planSet.add(other);
		}
		return planSet;
	}
	
	// --- HELPERS -------------------------------------------------------------
	
	/**
	 * Apply the given filters to the specified operator.
	 * 
	 * @param operator
	 * @param filters
	 */
	protected void applyFilters(BGPOperator<P, F> operator, Set<F> filters) {
		
		// copy given filters and remove all which are already applied
		Set<F> newFilters = new HashSet<F>(filters);
		newFilters.removeAll(BGPCollector.getFilters(operator));
		
		// check if node satisfies all variables for remaining filters
		Set<String> nodeVars = new VarsCollector().process(operator);
		Iterator<F> it = newFilters.iterator();
		while (it.hasNext()) {
			F filter = it.next();
			if (!nodeVars.containsAll(adapter.getFilterVars(filter)))
				it.remove();
		}
		
		operator.addFilters(newFilters);
	}
	
	// --- COLLECTORS ----------------------------------------------------------
	
	/**
	 * Collects all variables of contained access plan nodes.
	 */
	private class VarsCollector implements BGPVisitor<P, F> {
		
		private Set<String> vars = new HashSet<String>();
		
		public Set<String> process(BGPOperator<P, F> operator) {
			operator.accept(this);
			return this.vars;
		}

		@Override
		public void visit(AccessPlan<P, F> plan) {
			this.vars.addAll(adapter.getPatternVars(plan.getPattern()));
		}

		@Override
		public void visit(BGPJoin<P, F> join) {
			join.getLeft().accept(this);
			join.getRight().accept(this);
		}
		
	}
	
}
