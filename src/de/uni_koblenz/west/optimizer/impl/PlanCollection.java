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
package de.uni_koblenz.west.optimizer.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_koblenz.west.optimizer.Operator;

public class PlanCollection<O extends Operator> {
	
	private Map<Integer, Set<O>> planMap = new HashMap<Integer, Set<O>>();
	
	/**
	 * Adds the collection of query plans to the set of n-ary joins.
	 * @param plans the plans to add.
	 * @param n the number of n-ary joins.
	 */
	public void add(Set<O> plans, int n) {
		Set<O> planSet = planMap.get(n);
		if (planSet == null)
			planMap.put(n, planSet = new HashSet<O>());
		planSet.addAll(plans);
	}
	
	/**
	 * Return the collection of query plans for n-ary joins.
	 * @param n the number of n-ary joins.
	 * @return the collection of query plans.
	 */
	public Set<O> get(int n) {
		return this.planMap.get(n);
	}
}
