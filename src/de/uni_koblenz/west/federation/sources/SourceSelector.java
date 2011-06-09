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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.algebra.StatementPattern;

import de.uni_koblenz.west.federation.index.Graph;

/**
 * Interface for source selection strategies.
 * 
 * @author Olaf Goerlitz
 */
public interface SourceSelector {
	
	/**
	 * Select data sources which can contribute results for a set of SPARQL
	 * triple patterns.
	 * A data source may match multiple triple patterns and one triple pattern
	 * can obtain results from multiple data sources. Hence the mapping is a 
	 * N to N relation. 
	 * 
	 * @param patterns the SPARQL triple patterns which need to be matched.
	 * @return a map connecting data sources to triple patterns.
	 */
	public Map<Set<Graph>, List<StatementPattern>> getSources(Collection<StatementPattern> patterns);

}
