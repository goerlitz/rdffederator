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
package de.uni_koblenz.west.federation.evaluation;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.LookAheadIteration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

//import org.openrdf.cursor.Cursor;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
//import org.openrdf.query.EvaluationException;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
//import org.openrdf.store.StoreException;

/**
 * Hash join on two cursors.
 * 
 * @author Olaf Goerlitz
 */
//public class HashJoinCursor implements Cursor<BindingSet> {
public class HashJoinCursor extends LookAheadIteration<BindingSet, QueryEvaluationException> {
	
//	protected final Cursor<BindingSet> leftIter;
//	protected final Cursor<BindingSet> rightIter;
	protected final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;
	protected final CloseableIteration<BindingSet, QueryEvaluationException> rightIter;
	protected final String joinAttr;
	
	Deque<BindingSet> joinedBindings = new ArrayDeque<BindingSet>();
	protected HashMap<String, List<BindingSet>> hashMap;
	
	private volatile boolean closed;
	
//	public HashJoinCursor(Cursor<BindingSet> leftIter, Cursor<BindingSet> rightIter, Set<String> joinVars)
//			throws EvaluationException {
	public HashJoinCursor(CloseableIteration<BindingSet, QueryEvaluationException> leftIter, CloseableIteration<BindingSet, QueryEvaluationException> rightIter, Set<String> joinVars)
		throws QueryEvaluationException {

		if (joinVars.size() == 0)
			throw new UnsupportedOperationException("cross products not supported");
		if (joinVars.size() > 1)
			throw new UnsupportedOperationException("multiple join vars not supported");
		
		this.leftIter = leftIter;
		this.rightIter = rightIter;
		this.joinAttr = joinVars.iterator().next();
	}
	
	private void buildHashMap() {
		
		try {
			this.hashMap = new HashMap<String, List<BindingSet>>();
			BindingSet next;
			String joinValue;
			
			// TODO: handle cross product and multiple join variables
			
			// populate hash map with left side results
//			while (!closed && (next = leftIter.next()) != null) {
			while (!closed && leftIter.hasNext()) {
				next = leftIter.next();
				
				joinValue = next.getBinding(joinAttr).getValue().stringValue();
				List<BindingSet> bindings  = hashMap.get(joinValue);
				if (bindings == null) {
					bindings = new ArrayList<BindingSet>();
					hashMap.put(joinValue, bindings);
				}
				bindings.add(next);
			}
//		} catch (StoreException e) {
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Stop the evaluation and close any open cursor.
	 */
	@Override
//	public void close() throws StoreException {
	protected void handleClose() throws QueryEvaluationException {
		closed = true;

		// close left side cursor
		leftIter.close();
		rightIter.close();
	}

	@Override
//	public BindingSet next() throws StoreException {
	protected BindingSet getNextElement() throws QueryEvaluationException {
		
		if (hashMap == null)
			buildHashMap();
		
		// return next joined binding if available
		if (joinedBindings.size() != 0)
			return joinedBindings.remove();
		
		// or generate next join bindings
		// get next original binding set until join partner is found
		// TODO: handle cross product and multiple join variables
		List<BindingSet> bindings = null;
		BindingSet next = null;
		while (bindings == null) {
			
			// Sesame 3:
//			if ((next = rightIter.next()) == null)
//				return null;
			
			// Sesame 2:
			if (rightIter.hasNext())
				next = rightIter.next();
			else
				return null;
			
			// get the binding's join value and matching left side bindings
			String joinValue = next.getBinding(joinAttr).getValue().stringValue();
			bindings = hashMap.get(joinValue);
		}
		
		// create all join combinations
		for (BindingSet binding : bindings) {
			QueryBindingSet set = new QueryBindingSet(next);
			set.addAll(binding);
			joinedBindings.add(set);
		}
		
		return joinedBindings.remove();
	}

}
