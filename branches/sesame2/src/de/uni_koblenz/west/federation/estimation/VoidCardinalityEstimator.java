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
package de.uni_koblenz.west.federation.estimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.algebra.helpers.VarNameCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.federation.model.MappedStatementPattern;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * Cardinality estimation based on Void descriptions.
 * 
 * @author Olaf Goerlitz
 */
public class VoidCardinalityEstimator extends QueryModelVisitorBase<RuntimeException> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VoidCardinalityEstimator.class);
	
	protected RDFStatistics stats;
	
	protected Map<TupleExpr, Double> cardIndex = new HashMap<TupleExpr, Double>();
	
	public VoidCardinalityEstimator(RDFStatistics stats) {
		if (stats == null)
			throw new IllegalArgumentException("RDF stats must not be NULL.");
		
		this.stats = stats;
	}
	
	public Double process(TupleExpr expr) {
		synchronized (cardIndex) {
			cardIndex.clear();
			expr.visit(this);
			return cardIndex.get(expr);
		}
	}
	
	@Override
	public void meet(StatementPattern pattern) throws RuntimeException {
		if (pattern instanceof MappedStatementPattern)
			meet((MappedStatementPattern) pattern);
		else
			super.meet(pattern);
	}
	
	public void meet(MappedStatementPattern pattern) throws RuntimeException {
		
		// check cardinality index first
		if (cardIndex.get(pattern) != null)
			return;
		
		double card = 0;

		// assume that each source contributes distinct results: compute sum
		for (Graph source : pattern.getSources()) {
			card += getPatternCard(pattern, source).doubleValue();
		}
		
		// add cardinality to index
		cardIndex.put(pattern, card);
	}
	
	@Override
	public void meet(Filter filter) {
		
		// check cardinality index first
		if (cardIndex.get(filter) != null)
			return;
		
		// TODO: include condition in estimation
		// for now use same card as sub expression
		
		// add cardinality to index
		cardIndex.put(filter, cardIndex.get(filter.getArg()));
		
	}
	
	public void meet(Join join) throws RuntimeException {
		
		// check cardinality index first
		if (cardIndex.get(join) != null)
			return;
		
		// TODO: does the estimated cardinality depend on the current join?
		//       -> no: different join order should yield same cardinality
		
		// estimate cardinality of join arguments first
		join.getLeftArg().visit(this);
		join.getRightArg().visit(this);
		
		double joinSelectivity = getJoinSelectivity(join.getLeftArg(), join.getRightArg());
		
		double leftCard = cardIndex.get(join.getLeftArg());
		double rightCard = cardIndex.get(join.getRightArg());
		double card = joinSelectivity * leftCard * rightCard;

		// add cardinality to index
		cardIndex.put(join, card);
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Get the selectivity for a (named) variable in the supplied pattern.
	 * Computes the selectivity as 1/sum(card_i(P)) for all data sources 'i'. 
	 * 
	 * @param pattern the query pattern to process.
	 * @param varName the variable name.
	 * @return the selectivity of the variable.
	 */
	protected double getVarSelectivity(MappedStatementPattern pattern, String varName) {
		long count = 0;
		
		// TODO: this does not look right yet
		
		for (Graph source : pattern.getSources()) {
			
			if (varName.equals(pattern.getSubjectVar().getName())) {
				count += stats.distinctSubjects(source);
				continue;
			}
			if (varName.equals(pattern.getPredicateVar().getName())) {
				throw new UnsupportedOperationException("predicate join not supported yet");
			}
			if (varName.equals(pattern.getObjectVar().getName())) {
				count += stats.distinctObjects(source);
				continue;
			}
			throw new IllegalArgumentException("var name not found in pattern");
		}
		return 1.0 / count;
	}
	
	private double getJoinSelectivity(TupleExpr leftExpr, TupleExpr rightExpr) {
		
		// get join variables
		Set<String> joinVars = VarNameCollector.process(leftExpr);
		joinVars.retainAll(VarNameCollector.process(rightExpr));
		
		if (joinVars.size() == 0) {
			// cross product: selectivity is 1
			return 1.0;
		}
		if (joinVars.size() == 2) {
			// multi-valued join
			LOGGER.warn("join estimation for multiple vars not supported (yet) - using first var only");
		}
		
		// get all patterns which contain the join variables
		// TODO: extend to more than one join variable
		String joinVar = joinVars.iterator().next();
		List<StatementPattern> leftJoinPatterns = new ArrayList<StatementPattern>();
		List<StatementPattern> rightJoinPatterns = new ArrayList<StatementPattern>();
		
		for (StatementPattern pattern : StatementPatternCollector.process(leftExpr)) {
			if (VarNameCollector.process(pattern).contains(joinVar))
				leftJoinPatterns.add(pattern);
		}
		for (StatementPattern pattern : StatementPatternCollector.process(rightExpr)) {
			if (VarNameCollector.process(pattern).contains(joinVar))
				rightJoinPatterns.add(pattern);
		}
		
		// select one pattern from each join argument to define the join condition
		// TODO: analyze structure of join, current approach produces random estimations
		double leftSel = getVarSelectivity((MappedStatementPattern) leftJoinPatterns.get(0), joinVar);
		double rightSel = getVarSelectivity((MappedStatementPattern) rightJoinPatterns.get(0), joinVar);
		return Math.min(leftSel, rightSel);
	}
	
	private Number getPatternCard(MappedStatementPattern pattern, Graph source) {
		
		Value p = pattern.getPredicateVar().getValue();
		Value o = pattern.getObjectVar().getValue();
		
		// predicate must be bound
		if (p == null)
			throw new IllegalArgumentException("predicate must be bound: " + pattern);
		
		// handle rdf:type
		if (RDF.TYPE.equals(p) && o != null) {
			return stats.typeCard(source, o.stringValue());
		}

		// use triple count containing the predicate
		return stats.pCard(source, p.stringValue());
	}

}
