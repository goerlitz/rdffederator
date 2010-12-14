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
package de.uni_koblenz.west.model.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.Format;

/**
 * Graph statistics with separate statistics for subject, predicate, and object.
 * 
 * @author Olaf Goerlitz
 */
public class RDFStatistics implements GraphStatistics<String> {
	
	static final Logger LOGGER = LoggerFactory.getLogger(RDFStatistics.class);
	
	private DataStatistics<String> sStat;
	private DataStatistics<String> pStat;
	private DataStatistics<String> oStat;
	
	private long size;
	
	public RDFStatistics(long size, DataStatistics<String> sStat, DataStatistics<String> pStat, DataStatistics<String> oStat) {
		this.size  = size;
		this.sStat = sStat;
		this.pStat = pStat;
		this.oStat = oStat;
	}
	
//	/**
//	 * Create a new graph statistic from the individual S/P/O statistics.
//	 * 
//	 * @param size the size of the graph (#statements)
//	 * @param stats the individual S/P/O statistics as an array.
//	 */
//	public RDFStatistics(long size, List<DataStatistics<T>> stats) {
//		
//		// check input
//		if (stats.size() < 3)
//			throw new IllegalArgumentException("array needs three elements");
//		if (stats.get(0) == null || stats.get(1) == null || stats.get(2) == null)
//			throw new IllegalArgumentException("some array elements are null");
//		
////		if (stats.size() == 5) {
////			this.predStats = new ArrayList<DataStatistics<T>>();
////			this.predStats.add(stats.get(3));
////			this.predStats.add(stats.get(4));
////		}
//
//		this.size = size;
//		this.spoStats = stats;
//	}
	
//	/**
//	 * Create a new graph statistic from the individual S/P/O statistics.
//	 * 
//	 * @param size the size of the graph (#statements)
//	 * @param stats the individual S/P/O statistics as an array.
//	 */
//	public RDFStatistics(long size, List<DataStatistics<T>> stats, PredicateStatistics<T> multi) {
//		this(size, stats);
//		this.predStats = multi;
//	}
	
//	public void setPredCounts(CountStatistics<T> psCount, CountStatistics<T> poCount) {
//		this.predStats = new ArrayList<DataStatistics<T>>();
//		this.predStats.add(psCount);
//		this.predStats.add(poCount);
//	}
	
	@Override
	public long getGraphCardinality() {
		return this.size;
	}

	/**
	 * Return the selectivity of a pattern in the graph.
	 * 
	 * @param terms array containing the pattern constants.
	 * @return the selectivity.
	 */
	@Override
	public double getSelectivity(String[] terms) {
		
		// check input
		if (terms.length != 3)
			throw new IllegalArgumentException("array needs three elements");
		if (terms[0] == null && terms[1] == null && terms[2] == null)
			throw new IllegalArgumentException("all array elements are null");
		
		double patternSel = 1;
		
		// get subject/predicate/object cardinality if the term is not NULL
		for (int i = 0; i < 3; i++) {
			String term = terms[i];
			
			if (term == null)
				continue;
			
			DataStatistics<String> termStat = null;
			switch (i) {
			case 0: termStat = sStat; break;
			case 1: termStat = pStat; break;
			case 2: termStat = oStat; break;
			}
			
			double selectivity = 1;
//			if (i == 2 && predStats != null && terms[1] != null) {
////				selectivity = predStats.get(1).getCount(terms[1]);
//				predStats.getSubjectStats().selectivity(term);
//			} else {
				selectivity = termStat.selectivity(term);
//				selectivity = spoStats.get(i).selectivity(term);
//			}
			patternSel *= selectivity;
			
			LOGGER.debug("Sel[" + i + "]: " + Format.d(selectivity, 4));
		}
		return patternSel;
	}
	
	/**
	 * Export statistics.
	 * 
	 * @param exporter the destination.
	 */
	@Override
	public void serialize(StatExporter<String> exporter) throws IOException {
		
		exporter.writeSignature("MASTER=RDF,TRIPLES=" + size + ",SUBJECTS="
				+ sStat.size() + ",PREDICATES=" + pStat.size() + ",OBJECTS=" 
				+ oStat.size());

		exporter.writeStatistics(sStat, "SUBJECT");
		exporter.writeStatistics(pStat, "PREDICATE");
		exporter.writeStatistics(oStat, "OBJECT");
	}
	
}
