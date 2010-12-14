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
package de.uni_koblenz.west.optimizer;

import java.util.Set;

/**
 * Base class for query models based on the specified operator type.
 * 
 * @param <O> the query operator type used in the query model.
 * 
 * @author Olaf Goerlitz
 */
public interface QueryModel<O extends Operator> {
	
	/**
	 * Creates all basic access plans for the query model.
	 *  
	 * @return the set of basic access plans.
	 */
	public Set<O> createAccessPlans();
	
	/**
	 * Creates joins for the given combination of left and right operators.
	 * 
	 * @param leftOperator the left join operator.
	 * @param rightOperators the set of right join operators.
	 * @return the set of created joins.
	 */
	public Set<O> createJoins(O leftOperator, Set<O> rightOperators);
	
	/**
	 * Groups the given plans in equivalence class.
	 * 
	 * @param plans the set of plans to group.
	 * @return the sets of equivalent plans.
	 */
	public Set<Set<O>> groupEquivalentPlans(Set<O> plans);
	
	/**
	 * Filters complementary (distinct) plans by discarding overlapping plans.
	 *  
	 * @param plan the reference plan for comparison.
	 * @param candidatePlans the set of potential plans to check.
	 * @return the set of filtered plans.
	 */
	public Set<O> filterDistinctPlans(O plan, Set<O> candidatePlans);
	
	/**
	 * Returns the number of base relations in the model.
	 * 
	 * @return the number of base relations in the model.
	 */
	public int getBaseRelationCount();
	
	public void replaceRoot(O operator);
	
}
