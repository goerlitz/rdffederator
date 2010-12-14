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
package de.uni_koblenz.west.federation.adapter;

import java.util.List;

import org.openrdf.query.BindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.rdf.eval.SparqlExecutor;

public class SesameSparqlExecutor implements SparqlExecutor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SesameSparqlExecutor.class);
	
	@Override
	public long getResultSize(Graph graph, String query) {
		throw new UnsupportedOperationException("implementation needs fixing");
		
//		List<BindingSet> results = QueryExecutor.evalRemote(graph.toString(), query);
//		LOGGER.debug(results.size() + " results for " + graph + ": " + query);
//		return results.size();
	}
	
}
