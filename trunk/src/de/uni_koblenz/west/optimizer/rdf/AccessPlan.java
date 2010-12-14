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
package de.uni_koblenz.west.optimizer.rdf;

import java.util.Set;

import de.uni_koblenz.west.federation.index.Graph;

/**
 * An access plan based on a triple pattern.
 */
public class AccessPlan<P, F> extends BGPOperator<P, F> {
	
	private P pattern;
	private Set<Graph> sources;
	
	public AccessPlan(P pattern, Set<Graph> sources) {
		this.pattern = pattern;
		this.sources = sources;
	}
	
	public P getPattern() {
		return this.pattern;
	}
	
	public Set<Graph> getSources() {
		return this.sources;
	}
	
	// --- OVERRIDE --------------------------------------------------------
	
	@Override
	public void accept(BGPVisitor<P, F> visitor) {
		visitor.visit(this);
	}
	
}
