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
package de.uni_koblenz.west.optimizer.rdf.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.BGPVisitor;
import de.uni_koblenz.west.optimizer.rdf.ModelAdapter;

/**
 * Caching cardinality values.
 * 
 * @author Olaf Goerlitz
 */
public class CardinalityCache<P, F> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CardinalityCache.class);
	
	private Map<List<List<String>>, Number> valueMap = new HashMap<List<List<String>>, Number>();
	private PatternCollector collector = new PatternCollector();
	private ModelAdapter<P, F> adapter;
	
	public CardinalityCache(ModelAdapter<P, F> adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("model adapter must not be NULL.");
		this.adapter = adapter;
	}
	
	public Number getCard(BGPOperator<P, F> operator) {
		// get significant order of patterns for cardinality estimation
		// assuming that always the same filters are applied to a specific
		// pattern combination
		// TODO: if different filters occur with the same set of pattern, i.e.
		//       in case of different queries, the cardinality will be wrong
		List<List<String>> patternConstants = new ArrayList<List<String>>();
		for (P pattern : collector.getPatterns(operator)) {
			patternConstants.add(Arrays.asList(adapter.getPatternConstants(pattern)));
		}
		
		if (LOGGER.isDebugEnabled()) {
			Number card = valueMap.get(patternConstants);
			if (card != null)
				LOGGER.trace("cache hit (" + patternConstants.size() + " patterns): " + card);
			else
				LOGGER.debug("cache miss (" + patternConstants.size() + " patterns)");
			return card;
		}
		
		return valueMap.get(patternConstants);
	}
	
	public void setCard(BGPOperator<P, F> operator, Number card) {
		List<List<String>> patternConstants = new ArrayList<List<String>>();
		for (P pattern : collector.getPatterns(operator)) {
			patternConstants.add(Arrays.asList(adapter.getPatternConstants(pattern)));
		}
		// store value in cache
		valueMap.put(patternConstants, card);
	}
	
	/**
	 * Collects triple patterns in evaluation order.
	 * Cardinality estimation relies on the pattern order obtained by
	 * traversing the operation tree in a left-deep fashion. The actual
	 * tree structure is not important for the cardinality estimation as
	 * join cardinality computation always relies on two adjacent patterns
	 * independent of the tree level they are located at. 
	 */
	private class PatternCollector implements BGPVisitor<P, F> {
		
		private List<P> patterns;
		
		public List<P> getPatterns(BGPOperator<P, F> root) {
			this.patterns = new ArrayList<P>();
			root.accept(this);
			return this.patterns;
		}
		
		// -------------------------------------------------------------------------
		
		@Override
		public void visit(AccessPlan<P, F> plan) {
			this.patterns.add(plan.getPattern());
		}

		@Override
		public void visit(BGPJoin<P, F> join) {
			// descent into subtree left first
			join.getLeft().accept(this);
			join.getRight().accept(this);
		}
	}
	
}
