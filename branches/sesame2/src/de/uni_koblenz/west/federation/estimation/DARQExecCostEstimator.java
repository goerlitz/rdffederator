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
package de.uni_koblenz.west.federation.estimation;

import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.model.BindJoin;
import de.uni_koblenz.west.federation.model.HashJoin;
import de.uni_koblenz.west.federation.model.NestedLoopJoin;
import de.uni_koblenz.west.federation.model.RemoteQuery;

/**
 * Calculates the cost for executing the physical operators.
 * 
 * @author Olaf Goerlitz
 */
public class DARQExecCostEstimator extends AbstractExecCostEstimator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DARQExecCostEstimator.class);
	
	private static final int C_TRANSFER_QUERY = 1;
	private static final int C_TRANSFER_TUPLE = 1;
	
	private VoidCardinalityEstimator cardEst;
	
	public String getName() {
		return "DARQCost";
	}
	
	@Override
	public void meet(Join node) throws RuntimeException {
		
		if (node instanceof HashJoin) {
			meet((HashJoin) node);
		} else if (node instanceof BindJoin) {
			meet((BindJoin) node);
		} else if (node instanceof NestedLoopJoin) {
			meet((NestedLoopJoin) node);
		} else {
			throw new IllegalArgumentException();
		}
		super.meet(node);
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node)
			throws RuntimeException {
		
		// need to handle sub queries explicitly
		if (node instanceof RemoteQuery) {
			meet((RemoteQuery) node);
		} else {
			super.meetUnaryTupleOperator(node);
		}
	}
	
	// -------------------------------------------------------------------------
	
	protected void meet(HashJoin join) {
		throw new IllegalArgumentException("hash join is not implemented yet");
	}
	
	protected void meet(BindJoin join) {
		
		Double leftCard = cardEst.process(join.getLeftArg());
		Double joinCard = cardEst.process(join);
		
		this.cost += leftCard * (C_TRANSFER_TUPLE + C_TRANSFER_QUERY) + joinCard * C_TRANSFER_TUPLE;
		
	}
	
	protected void meet(NestedLoopJoin join) {
		Double leftCard = cardEst.process(join.getLeftArg());
		Double rightCard = cardEst.process(join.getLeftArg());
		
		this.cost += (leftCard + rightCard) * C_TRANSFER_TUPLE + 2 * C_TRANSFER_QUERY;
		
	}
	
	protected void meet(RemoteQuery query) {
		
		// no explicit cost for executing the sub query
		
	}

}
