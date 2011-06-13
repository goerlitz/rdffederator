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
package de.uni_koblenz.west.federation.sources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.index.Graph;

/**
 * Basic behavior of a source selector.
 * First aggregates the triple patterns in groups with same constant values.
 * Then the sources are selected and optimizations for sameAs may be applied.
 * 
 * @author Olaf Goerlitz
 */
public abstract class SourceSelectorBase implements SourceSelector {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceSelectorBase.class);
	
	private boolean attachSameAs;
	
	public SourceSelectorBase(boolean attachSameAs) {
		this.attachSameAs = attachSameAs;
	}
	
	/**
	 * Return all sources for the supplied pattern.
	 * 
	 * @param pattern
	 * @return a set of sources.
	 */
	protected abstract Set<Graph> getSources(StatementPattern pattern);
	
	/**
	 * Implementation of basic source selection.
	 */
	@Override
	public Map<Set<Graph>, List<StatementPattern>> getSources(Collection<StatementPattern> patterns) {
		
		Map<Set<Graph>, List<StatementPattern>> sourceMapping = new HashMap<Set<Graph>, List<StatementPattern>>();
		Map<List<StatementPattern>, Set<Graph>> sameAsMapping = new HashMap<List<StatementPattern>, Set<Graph>>();
		
		// aggregate patterns with the same constants (but different vars)
		TriplePatternIndex pso = new TriplePatternIndex();
		pso.add(patterns);
		
		// find sources for all distinct patterns
		for (List<StatementPattern> patternGroup : pso.getDistinctPatterns()) {
			
			// get sources for the first group pattern (with same constant values)
			StatementPattern pattern = patternGroup.get(0);
			Set<Graph> sources = getSources(pattern);
			
			// check returned sources
			if (sources.size() == 0) {
				LOGGER.warn("cannot find sources for: " + OperatorTreePrinter.print(pattern));
				continue;
			}
			
			// handle owl:sameAs triple patterns separately if necessary
			// (if the pattern's predicate is owl:sameAs and the subject variable is unbound) 
			if (attachSameAs && !pattern.getSubjectVar().hasValue()
					&& OWL.SAMEAS.equals(pattern.getPredicateVar().getValue())) {
				sameAsMapping.put(patternGroup, sources);
			} else {
				// store mapping of current patterns to sources
				List<StatementPattern> patternList = sourceMapping.get(sources);
				if (patternList == null) {
					patternList = new ArrayList<StatementPattern>();
					sourceMapping.put(sources, patternList);
				}
				patternList.addAll(patternGroup);				
			}
		}
		
		// Try to reorganize owl:sameAs patterns.
		// Combine patterns, if the subject variable of a owl:sameAs Pattern,
		// i.e. ?s in {?s owl:sameAs ?o} is found in the subject or object
		// position of another pattern, which is mapped to the same sources
		if (attachSameAs) {
			for (List<StatementPattern> sameAsList : sameAsMapping.keySet()) {
				Set<Graph> sameAsSources = sameAsMapping.get(sameAsList);
				
				for (StatementPattern sameAsPattern : sameAsList) {
					
					List<Set<Graph>> candidates = new ArrayList<Set<Graph>>();
					Var sameAsVar = sameAsPattern.getSubjectVar();
					
					// find patterns which contain the subject variable of the
					// same as pattern and 
					for (Set<Graph> sourceSet : sourceMapping.keySet()) {
						for (StatementPattern sp : sourceMapping.get(sourceSet)) {
							// check for same variable and if the sameAs pattern
							// is mapped to the same sources as the matched pattern
							if (containsSameAsVar(sameAsVar, sp) && sameAsSources.containsAll(sourceSet)) {
								candidates.add(sourceSet);
							}
						}
					}
					
					if (candidates.size() == 0) {
						LOGGER.warn("cannot reorganize: " + OperatorTreePrinter.print(sameAsPattern));
						
						List<StatementPattern> patternList = sourceMapping.get(sameAsSources);
						if (patternList == null) {
							patternList = new ArrayList<StatementPattern>();
							sourceMapping.put(sameAsSources, patternList);
						}
						patternList.add(sameAsPattern);
					} else {
						LOGGER.debug("reorganizing: " + OperatorTreePrinter.print(sameAsPattern));
						
						for (Set<Graph> sourceSet : candidates) {
							List<StatementPattern> patternList = sourceMapping.get(sourceSet);
							patternList.add(sameAsPattern);
						}
					}
				}
			}
		}
		
		return sourceMapping;
	}
	
	private boolean containsSameAsVar(Var sameAsVar, StatementPattern pattern) {
		String varName = sameAsVar.getName();
		Var sVar = pattern.getSubjectVar();
		Var oVar = pattern.getObjectVar();
		return (!sVar.hasValue() && sVar.getName().equals(varName))
				|| (!oVar.hasValue() && oVar.getName().equals(varName));
	}
	
}
