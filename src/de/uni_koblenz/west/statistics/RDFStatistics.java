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
import java.util.Set;

import de.uni_koblenz.west.federation.index.Graph;

/**
 * Interface for all RDF statistics provider.
 * 
 * @author Olaf Goerlitz
 */
public interface RDFStatistics {
	
	public Set<Graph> findSources(String sValue, String pValue, String oValue, boolean handleType);
	
	public long getSize(Graph g);
	
	public long distinctSubjects(Graph g);
	
	public long distinctSubjects(Graph g, URI predicate);
	
	public long distinctObjects(Graph g);
	
	public long distinctObjects(Graph g, URI predicate);
	
//	public Number patternCard(Graph g, URI s, URI p, RDFTerm o);
	
	public Number sCard(Graph g, URI subject);
	
	public Number pCard(Graph g, String predicate);
	
	public Number oCard(Graph g, RDFValue object);
	
	public Number poCard(Graph g, URI predicate, RDFValue object);
	
	public Number poCard(Graph g, URI predicate, RDFValue object, URI datatype);
	
	public Number spCard(Graph g, URI subject, URI predicate);
	
	public Number soCard(Graph g, URI subject, RDFValue object);
	
	public Number typeCard(Graph g, String type);
	
}
