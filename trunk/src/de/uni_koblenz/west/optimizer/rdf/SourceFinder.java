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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * @author Olaf Goerlitz
 */
public class SourceFinder<P> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceFinder.class);
	
	protected static final boolean HANDLE_SAMEAS = true;
	
	private ModelAdapter<P, ?> adapter;
	private RDFStatistics stats;
	
	/**
	 * Creates a source finder using the supplied statistics and model adapter.
	 * 
	 * @param stats the statistics to use.
	 * @param adapter the model adapter to use.
	 */
	public SourceFinder(RDFStatistics stats, ModelAdapter<P, ?> adapter) {
		this.adapter = adapter;
		this.stats = stats;
	}
	
	/**
	 * Find matching sources for the supplied patterns.
	 * Since a single pattern may match multiple sources the result maps
	 * a set of sources to a set of matched patterns, i.e {Graph}->{Pattern}.
	 * 
	 * @param patterns the pattern that need to matched to sources.
	 * @return a map that maps set of sources to sets of patterns.
	 */
	public Map<Set<Graph>, List<P>> findPlanSetsPerSource(Collection<P> patterns) {
		Map<Set<Graph>, List<P>> graphSets = new HashMap<Set<Graph>, List<P>>();
		List<P> sameAsPatterns = new ArrayList<P>();

		// find sources for all patterns and create Map for {Graph}->{Pattern}
		// patterns containing owl:sameAs may be treated separately
		for (P pattern : patterns) {
			String[] values = adapter.getPatternConstants(pattern);

			// add owl:sameAs patterns to extra list for special treatment
			if (HANDLE_SAMEAS && "http://www.w3.org/2002/07/owl#sameAs".equals(values[1])) {
				sameAsPatterns.add(pattern);
				continue;
			}

			// get sources for current pattern 
			Set<Graph> sources = stats.findSources(values[0], values[1], values[2]);
			
			// add current pattern with matched sources to the mapping
			List<P> patternList = graphSets.get(sources);
			if (patternList == null) {
				patternList = new ArrayList<P>();
				graphSets.put(sources, patternList);
			}
			patternList.add(pattern);
		}

		// special treatment of owl:sameAs patterns:
		// can be added to patterns which contain the subject variable
		// e.g. {x? a Type . x? sameAs ?y} or {ME knows ?x . x? sameAs ?y} 
		if (HANDLE_SAMEAS) {

			Iterator<P> iterator = sameAsPatterns.iterator();
			while (iterator.hasNext()) {
				
				P sameAsPattern = iterator.next();
				
				// get subject variable of owl:sameAs pattern and find other
				// patterns in the graph sets which contain this variable
				String varName = adapter.getVarName(sameAsPattern, 0);
				List<Set<Graph>> candidates = new ArrayList<Set<Graph>>();
				
				for (Set<Graph> graphSet : graphSets.keySet()) {
					for (P pattern : graphSets.get(graphSet)) {
						if (adapter.getPatternVars(pattern).contains(varName)) {
							candidates.add(graphSet);
						}
					}
				}

				// add the owl:sameAs pattern to all candidate sets and remove
				// it from the list
				for (Set<Graph> graphSet : candidates) {
					graphSets.get(graphSet).add(sameAsPattern);
					iterator.remove();
				}
			}
			
			// check that no owl:sameAs pattern is left.
			if (sameAsPatterns.size() != 0)
				throw new IllegalArgumentException("sameAs pattern can not be joined with another pattern: " + sameAsPatterns);

//			// find graph sets for unassigned sameAs patterns
//			for (P sameAsPattern : sameAsPatterns) {
//				String[] values = adapter.getPatternConstants(sameAsPattern);
//
//				// get sources for pattern and add pattern to the mapped list 
//				Set<Graph> sources = stats.findSources(values[0], values[1], values[2]);
//				List<P> patternList = graphSets.get(sources);
//				if (patternList == null) {
//					patternList = new ArrayList<P>();
//					graphSets.put(sources, patternList);
//				}
//				patternList.add(sameAsPattern);
//			}
		}

		// print pattern sets
		if (LOGGER.isDebugEnabled()) {
			for (Set<Graph> graphSet : graphSets.keySet()) {
				LOGGER.debug("BGPModel: Sources " + graphSet);
				for (P pattern : graphSets.get(graphSet))
					LOGGER.debug("  " + adapter.toSparqlPattern(pattern));
			}
		}
		
		return graphSets;
	}

}
