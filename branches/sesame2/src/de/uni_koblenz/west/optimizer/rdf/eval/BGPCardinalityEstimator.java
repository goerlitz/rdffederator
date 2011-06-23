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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.Format;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimator;
import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.BGPVisitor;
import de.uni_koblenz.west.optimizer.rdf.ModelAdapter;
import de.uni_koblenz.west.optimizer.rdf.util.BGPCollector;
import de.uni_koblenz.west.optimizer.rdf.util.CardinalityCache;
import de.uni_koblenz.west.statistics.RDFStatistics;
import de.uni_koblenz.west.statistics.RDFValue;
import de.uni_koblenz.west.vocabulary.RDF;

/**
 * Cardinality estimation for basic graph patterns.
 * 
 * @author Olaf Goerlitz
 *
 * @param <P> the triple pattern type.
 * @param <F> the filter type.
 */
public class BGPCardinalityEstimator<P, F> implements BGPModelEvaluator<P, F, Number>, CardinalityEstimator<BGPOperator<P, F>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BGPCardinalityEstimator.class);
	
	protected static final String RDF_TYPE = RDF.type.toString();
	
	protected Map<BGPOperator<P, F>, Number> cardMap = new HashMap<BGPOperator<P,F>, Number>();
	
	protected RDFStatistics stats;
	protected ModelAdapter<P, F> adapter;
	protected PatternFinder finder = new PatternFinder();
	protected CardinalityCache<P, F> cardCache;
	
	boolean useTypeStats = true;
	boolean useLiteralType = true;
	boolean usePAverage = true;
	
	int joinMisses = 0;
	
	public BGPCardinalityEstimator(RDFStatistics stats, ModelAdapter<P, F> adapter) {
		if (stats == null)
			throw new IllegalArgumentException("RDF stats must not be NULL.");
		if (adapter == null)
			throw new IllegalArgumentException("model adapter must not be NULL.");
		this.stats = stats;
		this.adapter = adapter;
		this.cardCache = new CardinalityCache<P, F>(adapter);
	}
	
	/**
	 * Returns the estimated cardinality for the given triple pattern.
	 * 
	 * @param source the data source used for the cardinality estimation.
	 * @param s the triple pattern subject.
	 * @param p the triple pattern predicate.
	 * @param o the triple pattern object.
	 * @return the pattern cardinality.
	 */
	protected Number getPatternCard(Graph source, URI s, URI p, RDFValue o) {
		// check if predicate is bound
		if (p != null) {

			// check if object is bound too
			if (o != null) {

				// if applicable get the type count
				if (useTypeStats && RDF_TYPE.equals(p.toString()) && o.isURI()) {
					String type = o.stringValue();
					LOGGER.trace("getting type card for: " + type);
					return stats.typeCard(source, type);
				}

				// check literal with datatype
				if (useLiteralType && o.hasDataType()) {
					LOGGER.warn("literal type handling not implemented yet");
					// TODO: use literal type information
				}
				
				long distObjects;

				// if applicable use average value per predicate
				if (usePAverage) {
					distObjects = stats.distinctObjects(source, p.toString());
				} else {
					distObjects = stats.distinctObjects(source);
				}

				// multiply with selectivity
				return stats.pCard(source, p.toString()).doubleValue() / distObjects;
			}

			// check if subject is bound too
			if (s != null) {
				
				long distSubjects;

				// if applicable use average value per predicate
				if (usePAverage) {
					distSubjects = stats.distinctSubjects(source, p.toString());
				} else {
					distSubjects = stats.distinctSubjects(source);
				}

				// multiply with selectivity
				return stats.pCard(source, p.toString()).doubleValue() / distSubjects;
			}

			// only predicate bound: use predicate count
			return stats.pCard(source, p.toString());
		}

		// check if subject is bound (with unbound predicate)
		if (s != null) {
			
			long size = stats.getSize(source);
			double card = (double) size / stats.distinctSubjects(source);

			// check if object is bound too
			if (o != null) {
				if (useLiteralType && o.hasDataType()) {
					LOGGER.warn("literal type handling not implemented yet");
					// TODO: handle typed literals
				}

				// else multiply individual selectivities
				return card / stats.distinctObjects(source);
			}
		}

		// check if only the object is bound
		if (o != null) {
			long size = stats.getSize(source);
			
			if (useLiteralType && o.hasDataType()) {
				LOGGER.warn("literal type handling not implemented yet");
				// TODO: handle typed literals
			} else {
				// multiply with average object selectivity
				return (double) size / stats.distinctObjects(source);
			}
		}
		return null;
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public Number eval(BGPOperator<P, F> operator) {
		
//		joinMisses = 0;
		
		// first look in cache
		Number card = cardCache.getCard(operator);
		if (card != null)
			return card;
		
		// compute cardinality
		synchronized(cardMap) {
			cardMap.clear();
			operator.accept(this);
			card = cardMap.get(operator);
		}
		
		// store cardinality in cache
		cardCache.setCard(operator, card);
		return card;
	}
	
	@Override
	public void visit(AccessPlan<P, F> plan) {
		
		// first look in cache
		Number card = cardCache.getCard(plan);
		if (card != null) {
			cardMap.put(plan, card);
			return;
		}
		
		double unionCard = 0;

		P pattern = plan.getPattern();
		Set<F> filters = plan.getFilters();
		Set<Graph> sources = plan.getSources();
		
		URI s = adapter.getSBinding(pattern);
		URI p = adapter.getPBinding(pattern);
		RDFValue o = adapter.getOBinding(pattern);
		
		// sanity check - need at least one bound variable
		if (s != null && p != null && o != null)
			throw new IllegalArgumentException("patterns not supported where all vars are bound");
		
		// compute cardinality of the pattern as union across sources
		for (Graph source : sources) {
			card = getPatternCard(source, s, p, o);
			unionCard += card.doubleValue();
			
//			// get selectivity for filters in source graph
//			//			c *= getSelectivity(filters, source);
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(" P-CARD: " + Format.d(unionCard, 2) + " ("
					+ adapter.toSparqlPattern(pattern) + ") @" + sources.size()
					+ " sources");
		}
		
		// store cardinality for this triple pattern
		cardMap.put(plan, unionCard);
	}
	
	@Override
	public void visit(BGPJoin<P, F> join) {
		
		// first look in cache
		Number card = cardCache.getCard(join);
		if (card != null) {
			cardMap.put(join, card);
			return;
		}
		
		// first process join arguments separately
		join.getLeft().accept(this);
		join.getRight().accept(this);
		
		// get join variable(s)
		Set<String> joinVars = new HashSet<String>();
		joinVars.addAll(VarCollector.getVars(join.getLeft(), adapter));
		joinVars.retainAll(VarCollector.getVars(join.getRight(), adapter));
		
		double selectivity = 1;
		
		// check for cross product
		if (joinVars.size() == 0) {
			LOGGER.info("join is cross product");
		} else {
			
			// check for multiple join vars
			if (joinVars.size() > 1)
				LOGGER.warn("join estimation for multiple vars not supported (yet) - using first var only");
//				throw new UnsupportedOperationException("joins over multiple vars not supported (yet)");
		
			// for each join argument get the patterns containing the join variable(s)
			Map<String, List<AccessPlan<P, F>>> leftList = finder.getPlan(join.getLeft(), joinVars);
			Map<String, List<AccessPlan<P, F>>> rightList = finder.getPlan(join.getRight(), joinVars);
			
			// select one pattern from each join argument to define the join condition
			String varName = joinVars.iterator().next();
			AccessPlan<P, F> left = leftList.get(varName).get(leftList.get(varName).size()-1);
			AccessPlan<P, F> right = rightList.get(varName).get(0);
			
			double leftSel = getVarSelectivity(left, varName);
			double rightSel = getVarSelectivity(right, varName);
			selectivity = Math.min(leftSel, rightSel);
			
			if (LOGGER.isDebugEnabled()) {
				for (String var : joinVars) {
					LOGGER.debug("JOIN VAR '" + var + "': " + BGPCollector.getPatterns(join).size() + " | " + leftList.get(var).size() + " <-> " + rightList.get(var).size() + "; left: " + left + "; right: " + right);
				}				
			}
		}
		
		double leftCard = (Double) cardMap.get(join.getLeft());
		double rightCard = (Double) cardMap.get(join.getRight());
		double resultCard = selectivity * leftCard * rightCard;
		
		if (LOGGER.isDebugEnabled()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Join on " + joinVars + " "
						+ Format.d(resultCard, 2) + " = "
						+ Format.d(leftCard, 2) + " * "
						+ Format.d(rightCard, 2) + " * "
						+ Format.d(selectivity, 5) + "\nLEFT:\n"
						+ adapter.toSparqlBGP(join.getLeft()) + "RIGHT:\n"
						+ adapter.toSparqlBGP(join.getRight()));
			}
		}
		
		cardMap.put(join, resultCard);
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Get the selectivity for a (named) variable in the supplied pattern.
	 * Computes the selectivity as 1/sum(card_i(P)) for all data sources 'i'. 
	 * 
	 * @param plan the access plan to use.
	 * @param varName the variable name.
	 * @return the selectivity of the variable.
	 */
	protected double getVarSelectivity(AccessPlan<P, F> plan, String varName) {
		long count = 0;
		
		for (Graph source : plan.getSources()) {
			
			int pos = adapter.getVarPosition(plan.getPattern(), varName);
			switch (pos) {
			case 1: // subject
				count += stats.distinctSubjects(source);
				break;
			case 2: // predicate
				throw new UnsupportedOperationException("predicate join not supported yet");
//				count += stats.distinctPredicates(source);
//				break;
			case 3: // object
				count += stats.distinctObjects(source);
				break;
			default: 
				throw new IllegalArgumentException("var name not found in pattern");
			}
		}
		return 1.0 / count;
	}
	
//	public Double getSelectivity(Set<F> filters, Graph source) {
//		// compute selectivity for filters
//		return 1.0;
//	}
	
	/**
	 * Visitor which returns the left-most or right-most triple pattern in
	 * an operator tree.
	 */
	class PatternFinder implements BGPVisitor<P, F> {
		
//		private boolean findFirst;
//		private AccessPlan<P, F> plan;
		private Set<String> vars;
		
		private Map<String, List<AccessPlan<P, F>>> plans;
		
		protected Map<String, List<AccessPlan<P, F>>> getPlan(BGPOperator<P, F> operator, Set<String> vars) {
			synchronized(this) {
//				this.findFirst = first;
				this.vars = vars;
				this.plans = new HashMap<String, List<AccessPlan<P,F>>>();
				for (String var : vars)
					this.plans.put(var, new ArrayList<AccessPlan<P,F>>());
//				this.plan = null;
				operator.accept(this);
				return this.plans;
//				return this.plan;
			}
		}
		
		@Override
		public void visit(AccessPlan<P, F> plan) {
			
			// check all join variables
			for (String var : vars) {
				// add plan if it contains the join variable
				if (adapter.getPatternVars(plan.getPattern()).contains(var))
					this.plans.get(var).add(plan);
			}
			
//			// found the wanted triple pattern, i.e. access plan
//			this.plan = plan;
		}

		@Override
		public void visit(BGPJoin<P, F> join) {
			// traverse left first
			join.getLeft().accept(this);
			join.getRight().accept(this);
			
//			// traverse left or right branch to find first or last pattern
//			if (findFirst) {
//				join.getLeft().accept(this);
////				if (vars.containsAll(adapter.getPatternVars(join.getLeft()));
//			} else
//				join.getRight().accept(this);
		}
		
	}
	
	static class VarCollector<P, F> implements BGPVisitor<P, F> {
		
		private Set<String> vars = new HashSet<String>();
		private ModelAdapter<P, F> adapter;
		
		public VarCollector(ModelAdapter<P, F> adapter) {
			this.adapter = adapter;
		}
		
		public static <P, F> Set<String> getVars(BGPOperator<P, F> node, ModelAdapter<P, F> adapter) {
			VarCollector<P, F> collector = new VarCollector<P, F>(adapter);
			node.accept(collector);
			return collector.vars;
		}
		
		public Set<String> getVars(BGPOperator<P, F> node) {
			synchronized (vars) {
				vars.clear();
				node.accept(this);
				return vars;
			}
		}
		
		@Override
		public void visit(AccessPlan<P, F> plan) {
			vars.addAll(adapter.getPatternVars(plan.getPattern()));
		}

		@Override
		public void visit(BGPJoin<P, F> join) {
			join.getLeft().accept(this);
			join.getRight().accept(this);
		}

	}
	
}
