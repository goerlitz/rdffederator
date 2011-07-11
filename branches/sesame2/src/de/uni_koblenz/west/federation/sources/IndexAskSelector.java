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

import java.util.List;
import java.util.Set;

import org.openrdf.query.algebra.StatementPattern;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * A source selector which first uses the index to find data sources which can
 * return results and then contacts the SPARQL Endpoints asking them if they
 * can really return results for a triple pattern. 
 * 
 * @author Olaf Goerlitz
 */
public class IndexAskSelector extends AskSelector {
	
	private IndexSelector indexSel;
	
	/**
	 * Creates a source finder using the supplied statistics and sources.
	 * 
	 * @param stats the statistics to use.
	 */
	public IndexAskSelector(RDFStatistics stats, boolean useTypeStats, List<Graph> sources) {
		super(sources);
		this.indexSel = new IndexSelector(stats, useTypeStats);
	}
	
	@Override
	protected Set<Graph> getSources(StatementPattern pattern) {
		Set<Graph> sources = this.indexSel.getSources(pattern);
		return getSources(pattern, sources);
	}
	
}
