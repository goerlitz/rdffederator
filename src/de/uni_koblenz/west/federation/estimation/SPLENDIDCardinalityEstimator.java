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

import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.federation.model.MappedStatementPattern;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * @author Olaf Goerlitz
 */
public class SPLENDIDCardinalityEstimator extends VoidCardinalityEstimator {
	
	boolean distSOPerPred;
	
	public SPLENDIDCardinalityEstimator(RDFStatistics stats, boolean distSOPerPred) {
		super(stats);
		this.distSOPerPred = distSOPerPred;
	}
	
	@Override
	public String getName() {
		return "SPLDCard";
	}
	
	@Override
	protected Number getPatternCard(MappedStatementPattern pattern, Graph source) {
		
		Value s = pattern.getSubjectVar().getValue();
		Value p = pattern.getPredicateVar().getValue();
		Value o = pattern.getObjectVar().getValue();
		
		// predicate must be bound
		if (p == null)
			throw new IllegalArgumentException("predicate must be bound: " + pattern);
		
		// handle rdf:type
		if (RDF.TYPE.equals(p) && o != null) {
			return stats.typeCard(source, o.stringValue());
		}
		
		Number pCard = stats.pCard(source, p.stringValue());
		
		// object is bound
		if (o != null) {
			if (distSOPerPred) {
				long distPredObj = stats.distinctObjects(source, p.stringValue());
				if (distPredObj == -1)
					throw new IllegalArgumentException("no value for distinct Objects per Predicate in statistics");
				return pCard.doubleValue() / distPredObj;
			} else {
				long pCount = stats.distinctPredicates(source);
				long distObj = stats.distinctObjects(source);
				return pCard.doubleValue() * pCount / distObj; 
			}
		}
		
		// subject is bound
		if (s != null) {
			if (distSOPerPred) {
				long distPredSubj = stats.distinctSubjects(source, p.stringValue());
				if (distPredSubj == -1)
					throw new IllegalArgumentException("no value for distinct Objects per Predicate in statistics");
				return pCard.doubleValue() / distPredSubj;
			} else {
				long pCount = stats.distinctPredicates(source);
				long distSubj = stats.distinctSubjects(source);
				return pCard.doubleValue() * pCount / distSubj; 
			}
			
		}

		// use triple count containing the predicate
		return pCard;
	}

}
