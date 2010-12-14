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
package de.uni_koblenz.west.federation;

import java.util.Arrays;
import java.util.List;

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.repository.Repository;

public class SourceFinder {
	
	List<Repository> members;
	
	public SourceFinder(Repository... repositories) {
		if (repositories == null || repositories.length == 0)
			throw new IllegalArgumentException("repositories must not be null.");
		this.members = Arrays.asList(repositories);
	}

	public SourceFinder(List<Repository> repositories) {
		if (repositories == null || repositories.size() == 0)
			throw new IllegalArgumentException("repositories must not be null.");
		this.members = repositories;
	}

	public List<Repository> getSources(StatementPattern pattern) {
		// TODO: select the appropriate members.
		return this.members;
	}

}
