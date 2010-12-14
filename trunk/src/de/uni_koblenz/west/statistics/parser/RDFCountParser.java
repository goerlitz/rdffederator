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
package de.uni_koblenz.west.statistics.parser;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.model.stats.AverageStatistics;
import de.uni_koblenz.west.model.stats.CountStatistics;
import de.uni_koblenz.west.model.stats.PredicateStatistics;
import de.uni_koblenz.west.model.stats.RDFStatistics;

/**
 * Collect subject, predicate, and object counts from the parsed RDF triples.
 *  
 * @author Olaf Goerlitz
 */
public class RDFCountParser extends RDFHandlerBase {
	
	static final Logger LOGGER = LoggerFactory.getLogger(RDFCountParser.class);
	
	// general counts for triples, subjects, predicates, and objects
	private long tripleCount = 0;
	private CountStatistics<String> sCount = new CountStatistics<String>();
	private CountStatistics<String> pCount = new CountStatistics<String>();
	private CountStatistics<String> oCount = new CountStatistics<String>();
	
	// counts for predicate details
	private PredicateStatistics<String> predStats;
	private CountStatistics<String> psCount;
	private CountStatistics<String> poCount;
	
	private boolean predicateDetails;
	private String currentPredicate;
	private int pass = 0;
	
	/**
	 * Create a new RDF parser.
	 * 
	 * @param predicateDetails if details for predicates should be collected.
	 */
	public RDFCountParser(boolean predicateDetails) {
		this.predicateDetails = predicateDetails;
	}
	
	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		
		String s = st.getSubject().stringValue();
		String p = st.getPredicate().stringValue();
		String o = st.getObject().stringValue();
		
		// collect general s,p,o counts in first pass
		if (pass == 0) {
			tripleCount++;
			sCount.inc(st.getSubject().stringValue());
			pCount.inc(st.getPredicate().stringValue());
			oCount.inc(st.getObject().stringValue());
			return;
		}
		
		// collect more detailed predicate counts if not first pass
		if (p.equals(this.currentPredicate)) {
			psCount.inc(s);
			poCount.inc(o);
		}
	}
	
	/**
	 * Returns true if another pass of the data parsing is required.
	 * 
	 * @return true if another pass of the data parsing is required.
	 */
	public boolean nextPass() {
		
		// only do more passes if predicate details should be collected
		if (!predicateDetails)
			return false;
			
		// initialize or store details from the last processed predicate
		if (this.currentPredicate == null) {
			pCount.sortByKey();
			this.predStats = new PredicateStatistics<String>();
		} else {
			AverageStatistics<String> psStat = new AverageStatistics<String>(psCount.size());
			AverageStatistics<String> poStat = new AverageStatistics<String>(poCount.size());
			predStats.set(this.currentPredicate, pCount.getCount(this.currentPredicate), psStat, poStat);
			
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Predicate pass " + pass + ": " + currentPredicate + " ["
						+ pCount.getCount(currentPredicate) + "] #S="
						+ psCount.size() + ", #O=" + poCount.size());
			}
		}
		
		// do another pass if there are not detailed counts for all predicates
		if (pass < pCount.size()) {
			psCount = new CountStatistics<String>();
			poCount = new CountStatistics<String>();
			this.currentPredicate = pCount.getElements().get(pass);
			this.pass++;
			return true;
		}
		return false;
	}

	/**
	 * Returns the collected RDF term statistics.
	 * 
	 * @return the collected RDF term statistics.
	 */
	public RDFStatistics getStatistics() {
		
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("#T=" + tripleCount + ", #S=" + sCount.size() + ", #P="
					+ pCount.size() + ", #O=" + oCount.size());
			LOGGER.info(sCount.metaStatsToString("SUBJECT"));
			LOGGER.info(pCount.metaStatsToString("PREDICATE"));
			LOGGER.info(oCount.metaStatsToString("OBJECT"));
			analyseCountDistribution();
		}
		
		if (this.predicateDetails)
			return new RDFStatistics(tripleCount, sCount, predStats, oCount);
		else 
			return new RDFStatistics(tripleCount, sCount, pCount, oCount);
	}
	
	private void analyseCountDistribution() {
		CountStatistics<Long> countDistr = new CountStatistics<Long>();
		for (String element : this.sCount.sortByValue().getElements()) {
			countDistr.inc(sCount.getCount(element));
		}
		LOGGER.info("#sCountDistr: " + countDistr.size());
//		for (Long element : countDistr.sortByKey().getElements()) {
//			LOGGER.info("count: " + element + ", freq: " + countDistr.getCount(element));
//		}
	}
}
