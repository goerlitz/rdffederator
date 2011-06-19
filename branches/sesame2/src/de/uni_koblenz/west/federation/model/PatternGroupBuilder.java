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
package de.uni_koblenz.west.federation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.index.Graph;


/**
 * @author Olaf Goerlitz
 */
public class PatternGroupBuilder {
	
	private boolean groupBySameAs;
	private boolean groupBySource;
	
	public PatternGroupBuilder(boolean groupBySource, boolean groupBySameAs) {
		this.groupBySource = groupBySource;
		this.groupBySameAs = groupBySameAs;
	}
	
	public void getGroups(List<MappedStatementPattern> patterns) {
		
		// sameAs grouping example:
		// ?city :population ?x  -> [A]
		// ?city :founded_in ?y  -> [B]
		// ?city owl:sameAs  ?z  -> [A,C,D]  ... group with first pattern only
		
		// 1. no grouping -> put each single pattern in a set
		// 2. group by source -> put pattern in set based on same source
		// 3. group by sameAs -> put sameAs pattern in set with matched pattern
		// 4. group by source and by sameAs
		
		List<MappedStatementPattern> sameAsPatterns = new ArrayList<MappedStatementPattern>();
		List<List<MappedStatementPattern>> patternGroups = new ArrayList<List<MappedStatementPattern>>();
		
		// start with grouping of sameAs patterns if applicable
		// this removes all sameAs patterns and matched pattern from the list
		if (groupBySameAs) {
			
			// move sameAs patterns from pattern list to sameAs pattern list
			Iterator<MappedStatementPattern> it = patterns.iterator();
			while (it.hasNext()) {
				MappedStatementPattern pattern = it.next();
				if (OWL.SAMEAS.equals(pattern.getPredicateVar().getValue()) && !pattern.getSubjectVar().hasValue()) {
					sameAsPatterns.add(pattern);
					it.remove();
				}
			}
			
			Set<MappedStatementPattern> matchedPatterns = new HashSet<MappedStatementPattern>();
			
			// find all matching pattern for each sameAs pattern
			for (MappedStatementPattern sameAsPattern : sameAsPatterns) {
				
				List<MappedStatementPattern> matchCandidates = new ArrayList<MappedStatementPattern>();
				Set<Graph> sameAsSources = sameAsPattern.getSources();
				Var sameAsSubjectVar = sameAsPattern.getSubjectVar();
				
				// find match candidates
				for (MappedStatementPattern pattern : patterns) {
					Set<Graph> patternSources = pattern.getSources();
					// check if pattern meets condition
					if (containsVar(pattern, sameAsSubjectVar) && sameAsSources.containsAll(patternSources)) {
						matchCandidates.add(pattern);
					}
				}
				
				// check if any match candidate was found
				if (matchCandidates.size() == 0) {
					// found no patterns to match with sameAs
					// add sameAs pattern as its own group
					List<MappedStatementPattern> group = new ArrayList<MappedStatementPattern>();
					group.add(sameAsPattern);
					patternGroups.add(group);
					continue;
				}
				
				// group match candidates with sameAs pattern
				for (MappedStatementPattern pattern : matchCandidates) {
					matchedPatterns.add(pattern);
					// add sameAs pattern with the matched pattern's Sources
					List<MappedStatementPattern> group = new ArrayList<MappedStatementPattern>();
					group.add(pattern);
					group.add(new MappedStatementPattern(sameAsPattern, pattern.getSources()));
					patternGroups.add(group);
				}
			}
			// removed all matched patterns from the pattern list
			patterns.removeAll(matchedPatterns);
		}
		
		if (groupBySource) {
			
			// create map for {Source}->{Pattern}
			Map<Set<Graph>, List<MappedStatementPattern>> sourceMap = new HashMap<Set<Graph>, List<MappedStatementPattern>>();
			
			// add all sameAs groups first, if applicable
			if (groupBySameAs) {
				for (List<MappedStatementPattern> patternList : patternGroups) {
					Set<Graph> sources = patternList.get(0).getSources();
					List<MappedStatementPattern> pList = sourceMap.get(sources);
					if (pList == null) {
						pList = new ArrayList<MappedStatementPattern>();
						sourceMap.put(sources, pList);
					}
					pList.addAll(patternList);
				}
			}
			
			// add all pattern from the list to the source mapping
			for (MappedStatementPattern pattern : patterns) {
				Set<Graph> sources = pattern.getSources();
				List<MappedStatementPattern> pList = sourceMap.get(sources);
				if (pList == null) {
					pList = new ArrayList<MappedStatementPattern>();
					sourceMap.put(sources, pList);
				}
				pList.add(pattern);
			}
			
			// finally create pattern groups for all source mappings
			patternGroups.clear();
			patternGroups.addAll(sourceMap.values());
			
		} else {
			
			// add all patterns from the list as individual group
			for (MappedStatementPattern pattern : patterns) {
				List<MappedStatementPattern> pList = new ArrayList<MappedStatementPattern>();
				pList.add(pattern);
				patternGroups.add(pList);
			}
			
		}
		
		// debugging
		for (List<MappedStatementPattern> pList : patternGroups) {
			StringBuffer buffer = new StringBuffer("Group [");
			Set<Graph> sources = null;
			for (MappedStatementPattern pattern : pList) {
				buffer.append(OperatorTreePrinter.print(pattern)).append(", ");
				if (sources == null) {
					sources = pattern.getSources();
				} else {
					if (!sources.equals(pattern.getSources())) 
						System.out.println("GroupBuilder: not the same sources: " + sources + " <-> " + pattern.getSources());
				}
			}
			buffer.setLength(buffer.length()-2);
			buffer.append("] @" + sources);
			System.out.println("GroupBuilder Groups: " + buffer.toString());
		}
		
	}

	private boolean containsVar(StatementPattern pattern, Var var) {
		String varName = var.getName();
		Var sVar = pattern.getSubjectVar();
		Var oVar = pattern.getObjectVar();
		return (!sVar.hasValue() && sVar.getName().equals(varName))
			|| (!oVar.hasValue() && oVar.getName().equals(varName));
	}

}
