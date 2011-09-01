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

import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimator;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.ModelAdapter;
import de.uni_koblenz.west.optimizer.rdf.util.BGPCollector;

/**
 * Returns the true cardinality of a query by counting the query results.
 * 
 * @author Olaf Goerlitz
 */
public class TrueCardinalityCounter<P, F> implements CardinalityEstimator<BGPOperator<P, F>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TrueCardinalityCounter.class);
	
	private SparqlExecutor evaluator;
	private ModelAdapter<P, F> adapter;
	
//	private HashMap<Set<P>, HashMap<Set<F>, Number>> cardinalities = new HashMap<Set<P>, HashMap<Set<F>, Number>>();
	
	public TrueCardinalityCounter(SparqlExecutor evaluator, ModelAdapter<P, F> adapter) {
		this.evaluator = evaluator;
		this.adapter = adapter;
	}
	
	@Override
	public Number eval(BGPOperator<P, F> operator) {
		
		Set<Graph> sources = BGPCollector.getSources(operator);
		
		if (sources.size() != 1)
			throw new UnsupportedOperationException("multiple sources not supported yet");
		
		String query = this.adapter.toSparqlBGP(operator);
		query = "SELECT DISTINCT * WHERE { \n" + query + "}";
		
		long size = this.evaluator.getResultSize(sources.iterator().next(), query);
		
//		LOGGER.debug("COST: " + size + " - " + query);
		
		return size;
		
		
//		Set<P> patterns = Collector.getPatterns(operator);
//		Set<F> filters = Collector.getFilters(operator);
//		
//		Number value = null;
//		HashMap<Set<F>, Number> filterMap = cardinalities.get(patterns);
//		if (filterMap != null) {
//			value = filterMap.get(filters);
//			if (value != null)
//				return value.doubleValue();
//		}
//		
//		String query = BGPSparqlWriter.write(operator, this.adapter);
//		query = "SELECT DISTINCT * WHERE { \n" + query + "}";
//		
////		long size = countResultTuples(query);
//		long size = this.source.getResultSize(query);
//
//		// add new result
//		if (filterMap == null) {
//			filterMap = new HashMap<Set<F>, Number>();
//			filterMap.put(filters, (Number) size);
//			cardinalities.put(patterns, filterMap);
//		}
////		if (value == null) {
////			
////		}
//		return size;
	}
	
}
