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

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.vocabulary.RDF;

/**
 * RDF statistics represented with Void 2 vocabulary.
 * 
 * @author Olaf Goerlitz
 */
public abstract class Void2Statistics implements RDFStatistics {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Void2Statistics.class);
	
	protected static final String VOID_PREFIX = "PREFIX void: <http://rdfs.org/ns/void#>\n";
	
	// -------------------------------------------------------------------------
	
	protected abstract List<String> evalVar(String query, String var);
	
	public abstract URI load(URL url) throws Exception;
	
	public abstract void setEndpoint(String endpoint, URI context);
	
	// -------------------------------------------------------------------------
	
	protected static final String concat(String... parts) {
		StringBuffer buf = new StringBuffer();
		for (String part : parts)
			buf.append(part);
		return buf.toString();
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public Set<Graph> findSources(String sValue, String pValue, String oValue, boolean handleType) {
		
		if (pValue == null)
			throw new UnsupportedOperationException("unbound predicates are not supported");
		
		String query = null;
		// query for RDF type occurrence if rdf:type with bound object is used
		if (handleType && RDF.type.toString().equals(pValue) && oValue != null) {
			query = concat(
					VOID_PREFIX,
					"SELECT ?source WHERE {",
					"  [] a void:Dataset ;",
					"     void:sparqlEndpoint ?source ;",
					"     void:classPartition ?part .",
					"  ?part void:class <", oValue, ">",
					"}");
		} else { // else query for predicate occurrence
			query = concat(
					VOID_PREFIX,
					"SELECT ?source WHERE {",
					"  [] a void:Dataset ;",
					"     void:sparqlEndpoint ?source ;",
					"     void:propertyPartition ?part .",
					"  ?part void:property <", pValue, "> .",
					"}");
		}
		
		// execute query and get all source bindings
		Set<Graph> sources = new HashSet<Graph>();
		for (String graph : evalVar(query, "source")) {
			sources.add(new Graph(graph));
		}
		return sources;
	}
	
	@Override
	public long getSize(Graph g) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?size WHERE {",
				"  [] a void:Dataset ;",
				"     void:triples ?size ;",
				"     void:sparqlEndpoint <", g.toString(), "> .",
				"}");
		
		List<String> bindings = evalVar(query, "size");
		if (bindings.size() == 0) {
			LOGGER.info("unable to find size of graph: " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple size values for graph: " + g.toString());
		return Long.parseLong(bindings.get(0));
	}
	
	@Override
	public Number typeCard(Graph g, URI type) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?card WHERE {",
				"  [] a void:Dataset ;",
				"     void:sparqlEndpoint <", g.toString(), "> ;",
				"     void:classPartition ?part .",
				"  ?part void:class <", type.toString(), "> ;",
				"        void:entities ?card .",
				"}");
		
		List<String> bindings = evalVar(query, "card");
		if (bindings.size() == 0) {
			LOGGER.debug("unable to find cardinality for type " + type + " in graph " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple cardinality values for type " + type + " in graph: " + g.toString());
		return Long.parseLong(bindings.get(0));
	}
	
	public long distinctSubjects(Graph g) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?subjects WHERE {",
				"  [] a void:Dataset ;",
				"     void:sparqlEndpoint <", g.toString(), "> ;",
				"     void:distinctSubjects ?subjects .",
				"}");
		
		List<String> bindings = evalVar(query, "subjects");
		if (bindings.size() == 0) {
			LOGGER.info("unable to find number of distinct subjects in graph " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple distinct subjects for endpoint: " + g.toString() + ". There exists probably more than one void decription for it.");
		
		long value = 0;
		for (String count : bindings)
			value += Long.parseLong(count);
		return value;
	}
	
	@Override
	public long distinctSubjects(Graph g, URI predicate) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?subjects WHERE {",
				"  [] a void:Dataset ;",
				"     void:sparqlEndpoint <", g.toString(), "> ;",
				"     void:propertyPartition ?part .",
				"  ?part void:property <", predicate.toString(), "> ;",
				"        void:distinctSubjects ?subjects .",
				"}");
		
		List<String> bindings = evalVar(query, "subjects");
		if (bindings.size() == 0) {
			LOGGER.info("failed to find number of distinct subjects for predicate " + predicate + " in graph " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple numbers of distinct subjects for predicate " + predicate + " in graph " + g.toString());
		return Long.parseLong(bindings.get(0));
	}
	
	public long distinctObjects(Graph g) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?objects WHERE {",
				"  [] a void:Dataset ;",
				"     void:sparqlEndpoint <", g.toString(), "> ;",
				"     void:distinctObjects ?objects .",
				"}");
		
		List<String> bindings = evalVar(query, "objects");
		if (bindings.size() == 0) {
			LOGGER.info("unable to find number of distinct objects in graph " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple numbers for distinct objects in graph: " + g.toString());
		return Long.parseLong(bindings.get(0));
	}

	@Override
	public long distinctObjects(Graph g, URI predicate) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?objects WHERE {",
				"  [] a void:Dataset ;",
				"     void:sparqlEndpoint <", g.toString(), "> ;",
				"     void:propertyPartition ?part .",
				"  ?part void:property <", predicate.toString(), "> ;",
				"        void:distinctObjects ?objects .",
				"}");
		
		List<String> bindings = evalVar(query, "objects");
		if (bindings.size() == 0) {
			LOGGER.info("failed to find number of distinct objects for predicate " + predicate + " in graph " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple numbers of distinct objects for predicate " + predicate + " in graph " + g.toString());
		return Long.parseLong(bindings.get(0));
	}
	
	@Override
	public Number sCard(Graph g, URI subject) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	@Override
	public Number pCard(Graph g, URI predicate) {
		String query = concat(
				VOID_PREFIX,
				"SELECT ?card WHERE {",
				"  [] a void:Dataset ;",
				"     void:sparqlEndpoint <", g.toString(), "> ;",
				"     void:propertyPartition ?part .",
				"  ?part void:property <", predicate.toString(), "> ;",
				"        void:triples ?card .",
				"}");
		
		List<String> bindings = evalVar(query, "card");
		if (bindings.size() == 0) {
			LOGGER.info("failed to find cardinality for predicate " + predicate + " in graph " + g.toString());
			return -1;
		}
		if (bindings.size() > 1)
			LOGGER.warn("found multiple cardinality values for predicate " + predicate + " in graph " + g.toString());
		return Long.parseLong(bindings.get(0));
	}
	
	@Override
	public Number oCard(Graph g, RDFValue object) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Number poCard(Graph g, URI predicate, RDFValue object) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public Number poCard(Graph g, URI predicate, RDFValue object, URI datatype) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Number soCard(Graph g, URI subject, RDFValue object) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Number spCard(Graph g, URI subject, URI predicate) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
}
