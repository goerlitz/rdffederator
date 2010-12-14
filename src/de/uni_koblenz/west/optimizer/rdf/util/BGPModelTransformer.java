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
package de.uni_koblenz.west.optimizer.rdf.util;

import java.util.Stack;

import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.eval.BGPModelEvaluator;

/**
 * A model transformer based on a model visitor.
 * Uses map functions to transform a generic Basic Graph Pattern model
 * to a Basic Graph Pattern model of the specified model node type.
 * 
 * @author Olaf Goerlitz
 *
 * @param <T> the specific tree model node type.
 * @param <P> the used pattern type.
 * @param <F> the used filter type.
 */
public abstract class BGPModelTransformer<T, P, F> implements BGPModelEvaluator<P, F, T> {
	
	private Stack<T> stack = new Stack<T>();
	
	// --- ABSTRACT --------------------------------------------------------
	
	protected abstract T mapPattern(P pattern);
	
	protected abstract T mapFilter(F filter, T node);
	
	protected abstract T mapJoin(T left, T right);
	
	// --- OVERRIDE --------------------------------------------------------
	
	@Override
	public T eval(BGPOperator<P, F> operator) {
		synchronized(this) {
			operator.accept(this);
			if (this.stack.size() != 1)
			throw new IllegalStateException("result stack has "
					+ this.stack.size() + " elements.");
			return this.stack.pop();
		}
	}

	@Override
	public void visit(AccessPlan<P, F> plan) {
		
		T result = mapPattern(plan.getPattern());
		
		for (F filter : plan.getFilters()) {
			result = mapFilter(filter, result);
		}
		
		stack.add(result);
	}

	@Override
	public void visit(BGPJoin<P, F> join) {
		
		join.getLeft().accept(this);
		join.getRight().accept(this);
		T right = this.stack.pop();
		T left = this.stack.pop();
		
		T result = mapJoin(left, right);
		
		for (F filter : join.getFilters()) {
			result = mapFilter(filter, result);
		}
		
		stack.add(result);
	}
	
}
