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
package de.uni_koblenz.west.federation.sources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.federation.index.Graph;

/**
 * A source selector which contacts all SPARQL endpoints asking them whether
 * they can answer the given triple pattern or not. 
 * 
 * @author Olaf Goerlitz
 */
public class SparqlAskSelector extends SourceSelectorBase {
	
	private List<Graph> sources;
	
	/**
	 * Creates a source selector using the supplied statistics and model adapter.
	 * 
	 * @param adapter the model adapter to use.
	 * @param sources the list of data sources to contact. 
	 */
	public SparqlAskSelector(List<Graph> sources) {
		this.sources = sources;
	}

	@Override
	public Set<Graph> getSources(StatementPattern pattern) {
		Set<Graph> sourceSet = new HashSet<Graph>();
		
		// ask each source for current pattern
		for (Graph source : sources) {
			String sparql = OperatorTreePrinter.print(pattern);
			if (QueryExecutor.ask(source.toString(), sparql))
				sourceSet.add(source);
		}
		return sourceSet;
	}

	public static void main(String[] args) {
		String query = "SELECT * WHERE { ?x a <http://xmlns.com/foaf/0.1/Person> } ";
		List<Graph> sources = Arrays.asList( new Graph[] { new Graph("http://dbpedia.org/sparql") } );
		
		SPARQLParser parser = new SPARQLParser();
		List<StatementPattern> patterns;
		try {
			ParsedQuery model = parser.parseQuery(query, null);
			patterns = StatementPatternCollector.process(model.getTupleExpr());
			SparqlAskSelector ask = new SparqlAskSelector(sources);
			Map<Set<Graph>, List<StatementPattern>> map = ask.getSources(patterns);
			
			// print results
			for (Set<Graph> key : map.keySet()) {
				System.out.println(key + " -> " + map.get(key));
			}
			
		} catch (MalformedQueryException e) {
			e.printStackTrace();
		}
	}

}
