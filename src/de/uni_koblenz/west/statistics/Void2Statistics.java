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
package de.uni_koblenz.west.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.vocabulary.RDF;
import de.uni_koblenz.west.vocabulary.VOID2;

/**
 * RDF statistics represented with voiD vocabulary.
 * 
 * @author Olaf Goerlitz
 */
public abstract class Void2Statistics implements RDFStatistics {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Void2Statistics.class);
	
	private static final String VOID_PREFIX = "PREFIX void: <" + VOID2.NAMESPACE + ">\n";
	
	private static final String VAR_GRAPH = "$GRAPH$";
	private static final String VAR_TYPE  = "$TYPE$";
	private static final String VAR_PRED  = "$PRED$";

	private static final String PRED_SOURCE = VOID_PREFIX +
			"SELECT ?source WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint ?source ;" +
			"     void:propertyPartition ?part ." +
			"  ?part void:property <" + VAR_PRED + "> ." +
			"}";
	
	private static final String TYPE_SOURCE = VOID_PREFIX +
			"SELECT ?source WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint ?source ;" +
			"     void:classPartition ?part ." +
			"  ?part void:class <" + VAR_TYPE + ">" +
			"}";
	
	private static final String TRIPLE_COUNT = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:triples ?count ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ." +
			"}";
	
	private static final String DISTINCT_PREDICATES = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:properties ?count ." +
			"}";

	private static final String DISTINCT_SUBJECTS = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:distinctSubjects ?count ." +
			"}";

	private static final String DISTINCT_PRED_SUBJECTS = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:propertyPartition ?part ." +
			"  ?part void:property <" + VAR_PRED + "> ;" +
			"        void:distinctSubjects ?count ." +
			"}";
	
	private static final String DISTINCT_OBJECTS = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:distinctObjects ?count ." +
			"}";
	
	private static final String DISTINCT_PRED_OBJECTS = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:propertyPartition ?part ." +
			"  ?part void:property <" + VAR_PRED + "> ;" +
			"        void:distinctObjects ?count ." +
			"}";
	
	private static final String TYPE_TRIPLES = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:classPartition ?part ." +
			"  ?part void:class <" + VAR_TYPE + "> ;" +
			"        void:entities ?count ." +
			"}";
	
	private static final String PRED_TRIPLES = VOID_PREFIX +
			"SELECT ?count WHERE {" +
			"  [] a void:Dataset ;" +
			"     void:sparqlEndpoint <" + VAR_GRAPH + "> ;" +
			"     void:propertyPartition ?part ." +
			"  ?part void:property <" + VAR_PRED + "> ;" +
			"        void:triples ?count ." +
			"}";
	
	// -------------------------------------------------------------------------
	
	protected abstract List<String> evalVar(String query, String var);
	
	public abstract URI load(URI voidURI, URI endpoint) throws Exception;
	
	public abstract List<Graph> getEndpoints();
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the count value defined by the supplied query and variable substitutions.
	 * 
	 * @param query the query to be executed on the voiD repository.
	 * @param vars the variable bindings to be substituted in the query.
	 * @return the resulting count value.
	 */
	private long getCount(String query, String... vars) {
		
		// replace query variables
		for (int i = 0; i < vars.length; i++) {
			query = query.replace(vars[i], vars[++i]);
		}
		
		List<String> bindings = evalVar(query, "count");
		
		// check result validity
		if (bindings.size() == 0) {
			LOGGER.warn("found no count for " + vars);
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple counts for " + vars);
		
		return Long.parseLong(bindings.get(0));
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public Set<Graph> findSources(String sValue, String pValue, String oValue, boolean handleType) {
		
		Set<Graph> sources = new HashSet<Graph>();
		
		if (pValue == null) {
			LOGGER.info("found triple pattern with unbound predicate: selecting all sources");
			sources.addAll(getEndpoints());
			return sources;
		}
		
		String query = null;
		// query for RDF type occurrence if rdf:type with bound object is used
		if (handleType && RDF.type.toString().equals(pValue) && oValue != null) {
			query = TYPE_SOURCE.replace(VAR_TYPE, oValue);
		} else { // else query for predicate occurrence
			query = PRED_SOURCE.replace(VAR_PRED, pValue);
		}
		
		// execute query and get all source bindings
		for (String graph : evalVar(query, "source")) {
			sources.add(new Graph(graph));
		}
		return sources;
	}
	
	@Override
	public long getTripleCount(Graph g) {
		return getCount(TRIPLE_COUNT, VAR_GRAPH, g.toString());
	}
	
	@Override
	public long getPredicateCount(Graph g, String predicate) {
		return getCount(PRED_TRIPLES, VAR_GRAPH, g.toString(), VAR_PRED, predicate);
	}
	
	@Override
	public long getTypeCount(Graph g, String type) {
		return getCount(TYPE_TRIPLES, VAR_GRAPH, g.toString(), VAR_TYPE, type);
	}
	
	@Override
	public long getDistinctPredicates(Graph g) {
		return getCount(DISTINCT_PREDICATES, VAR_GRAPH, g.toString());
	}
	
	@Override
	public long getDistinctSubjects(Graph g) {
		return getCount(DISTINCT_SUBJECTS, VAR_GRAPH, g.toString());
	}
	
	@Override
	public long getDistinctSubjects(Graph g, String predicate) {
		return getCount(DISTINCT_PRED_SUBJECTS, VAR_GRAPH, g.toString(), VAR_PRED, predicate);
	}
	
	@Override
	public long getDistinctObjects(Graph g) {
		return getCount(DISTINCT_OBJECTS, VAR_GRAPH, g.toString());
	}

	@Override
	public long getDistinctObjects(Graph g, String predicate) {
		return getCount(DISTINCT_PRED_OBJECTS, VAR_GRAPH, g.toString(), VAR_PRED, predicate);
	}
	
}
