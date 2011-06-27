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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.algebra.helpers.VarNameCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.federation.model.MappedStatementPattern;
import de.uni_koblenz.west.federation.model.RemoteQuery;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * Cardinality estimation based on Void descriptions.
 * 
 * @author Olaf Goerlitz
 */
public abstract class VoidCardinalityEstimator extends AbstractCardinalityEstimator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VoidCardinalityEstimator.class);
	
	protected RDFStatistics stats;
	
	// -------------------------------------------------------------------------
	
	protected abstract Number getPatternCard(MappedStatementPattern pattern, Graph source);
	
	// -------------------------------------------------------------------------
	
	public VoidCardinalityEstimator(RDFStatistics stats) {
		if (stats == null)
			throw new IllegalArgumentException("RDF stats must not be NULL.");
		
		this.stats = stats;
	}
	
	public void meet(MappedStatementPattern pattern) throws RuntimeException {
		
		// check cardinality index first
		if (getIndexCard(pattern) != null)
			return;
		
		double card = 0;

		// estimate the cardinality of the pattern for each source and sum up
		// assumes that all result tuple will be distinct
		for (Graph source : pattern.getSources()) {
			card += getPatternCard(pattern, source).doubleValue();
		}
		
		// add cardinality to index
		setIndexCard(pattern, card);
	}
	
//	protected void meet(RemoteQuery node) {
//		
//		// check cardinality index first
//		if (getIndexCard(node) != null)
//			return;
//		
//		Map<String, List<StatementPattern>> patternGroup = new HashMap<String, List<StatementPattern>>();
//		
//		// group patterns by subject
//		for (StatementPattern p : StatementPatternCollector.process(node.getArg())) {
//			
//			// get cardinality first
//			meet(p);
//			
//			String varName = p.getSubjectVar().getName();
//			List<StatementPattern> pList = patternGroup.get(varName);
//			if (pList == null) {
//				pList = new ArrayList<StatementPattern>();
//				patternGroup.put(varName, pList);
//			}
//			pList.add(p);
//		}
//		
//		// process each pattern group: start with patterns where P+O is bound
//		for (String varName : patternGroup.keySet()) {
//
//			List<StatementPattern> pList = patternGroup.get(varName);
//			LOGGER.warn("PGroup: " + varName + " -> " + pList);
//			
//			// find minimum cardinality for all pattern which have P+O bound
//			Double minCard = Double.POSITIVE_INFINITY;
//			Iterator<StatementPattern> it = pList.iterator();
//			while (it.hasNext()) {
//				StatementPattern p = it.next();
//				if (p.getObjectVar().getValue() != null) {
//					Double pCard = getIndexCard(p);
//					if (pCard.compareTo(minCard) < 0) {
//						minCard = pCard;
//					}
//					it.remove();
//				}
//			}
//			
//			// check if we have found a minimum cardinality
//			if (minCard.equals(Double.POSITIVE_INFINITY)) {
//				LOGGER.warn("no pattern with bound P+O for " + varName);
//				minCard = 1d;
//			}
//			
//			// the remaining patterns are multiplied like a cross product
//			for (StatementPattern p : pList) {
//				minCard = minCard * getIndexCard(p);
//				double varSel = getVarSelectivity((MappedStatementPattern) p, varName);
//				LOGGER.warn("varsel: " + varSel + " ++ " + p);
//			}
//			
//			LOGGER.warn("PGroup: " + varName + " -> " + minCard);
//			
//		}
//		
//		
//		node.getArg().visit(this);
//		// add same cardinality as child argument
//		setIndexCard(node, getIndexCard(node.getArg()));
//	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public void meet(StatementPattern pattern) throws RuntimeException {
		if (pattern instanceof MappedStatementPattern)
			meet((MappedStatementPattern) pattern);
		else
			throw new IllegalArgumentException("cannot estimate cardinality for triple pattern without sources: " + pattern);
	}
	
	public void meet(Join join) throws RuntimeException {
		
		// check cardinality index first
		if (getIndexCard(join) != null)
			return;
		
		// TODO: does the estimated cardinality depend on the current join?
		//       -> no: different join order should yield same cardinality
		
		// estimate cardinality of join arguments first
		join.getLeftArg().visit(this);
		join.getRightArg().visit(this);
		
		double joinSelectivity = getJoinSelectivity(join.getLeftArg(), join.getRightArg());
		
		double leftCard = getIndexCard(join.getLeftArg());
		double rightCard = getIndexCard(join.getRightArg());
		double card = joinSelectivity * leftCard * rightCard;

		// add cardinality to index
		setIndexCard(join, card);
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
			
			Long pCount = stats.distinctPredicates(source);
			
			if (varName.equals(pattern.getSubjectVar().getName())) {
				count += stats.distinctSubjects(source);
//				count += stats.distinctSubjects(source) / pCount.doubleValue();
				continue;
			}
			if (varName.equals(pattern.getPredicateVar().getName())) {
				throw new UnsupportedOperationException("predicate join not supported yet");
			}
			if (varName.equals(pattern.getObjectVar().getName())) {
				count += stats.distinctObjects(source);
//				count += stats.distinctObjects(source) / pCount.doubleValue();
				continue;
			}
			throw new IllegalArgumentException("var name not found in pattern");
		}
		return 1.0 / count;
	}
	
	protected double getJoinSelectivity(TupleExpr leftExpr, TupleExpr rightExpr) {
		
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
	
}
