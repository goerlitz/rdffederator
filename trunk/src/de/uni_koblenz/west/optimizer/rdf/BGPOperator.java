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

import java.util.HashSet;
import java.util.Set;

import de.uni_koblenz.west.optimizer.Operator;

/**
 * Generic Basic Graph Pattern node including a set of assigned filters.
 */
public abstract class BGPOperator<P, F> implements Operator {
	
	protected Set<F> filters = new HashSet<F>();
	
	// --- ABSTRACT ------------------------------------------------------------
	
	public abstract void accept(BGPVisitor<P, F> visitor);
	
	// -------------------------------------------------------------------------
	
	/**
	 * Assigns a set of filters to the operator.
	 * 
	 * @param filters the filters to add.
	 */
	protected final void addFilters(Set<F> filters) {
		if (filters != null && !filters.isEmpty())
			this.filters.addAll(filters);
	}
	
	/**
	 * Returns the set of filters assigned to the operator.
	 * 
	 * @return the set of assigned filters
	 */
	public final Set<F> getFilters() {
		return this.filters;
	}
	
}
