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
package de.uni_koblenz.west.federation.helpers;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

import de.uni_koblenz.west.federation.adapter.SesameBGPWrapper;

/**
 * A visitor which collects all basic graph patterns (BGP) from a query model.
 * 
 * @author Olaf Goerlitz
 */
public class BasicGraphPatternCollector extends QueryModelVisitorBase<RuntimeException> {
	
	private List<SesameBGPWrapper> bgpList = new ArrayList<SesameBGPWrapper>();
	private List<StatementPattern> patterns = new ArrayList<StatementPattern>();
	private List<ValueExpr> filters = new ArrayList<ValueExpr>();
	
	public List<SesameBGPWrapper> eval(QueryModelNode node) {
		synchronized (this) {
			this.bgpList.clear();
			this.patterns.clear();
			this.filters.clear();
			node.visit(this);
			return bgpList;
		}
	}

	private void saveBGP(QueryModelNode current, QueryModelNode parent) {
		if (patterns.size() == 0)
			throw new IllegalArgumentException("found BGP without patterns");
		bgpList.add(new SesameBGPWrapper(current, parent, patterns, filters));
		patterns.clear();
		filters.clear();
	}
	
	// --------------------------------------------------------------

	@Override
	public void meet(Filter filter) throws RuntimeException {
		
		// handle filtered arguments first
		super.meet(filter);
		
		// TODO: need to check for valid child node
		
		// check if the parent node is a valid BGP node (Join or Filter)
		// because invalid BGP (parent) nodes are not visited
		QueryModelNode parent = filter.getParentNode();
		if (parent instanceof Join || parent instanceof Filter) {
			this.filters.add(filter.getCondition());
			return;
		}
		// otherwise check if filter is a direct child of a Projection
		// TODO: what if parent is Limit or OrderBy?
		if (parent instanceof Projection || parent instanceof MultiProjection) {
			this.filters.add(filter.getCondition());
		}
		saveBGP(filter, parent);
	}

	@Override
	public void meet(StatementPattern pattern) throws RuntimeException {
		
		// check if the parent node is a valid BGP node (Join or Filter)
		// because invalid BGP (parent) nodes are not visited
		QueryModelNode parent = pattern.getParentNode();
		if (parent instanceof Join || parent instanceof Filter) {
			this.patterns.add(pattern);
		}
		// otherwise skip pattern - single triple patterns need no optimization
	}
	
	@Override
	public void meet(Join join) throws RuntimeException {
		
		boolean valid = true;
//		List<? extends TupleExpr> joinArgs = join.getArgs();
		TupleExpr[] joinArgs = { join.getLeftArg(), join.getRightArg()};
		
		// First check if Join is a valid BGP - only if all join arguments are valid BGPs
		for (TupleExpr expr : joinArgs) {
			if (expr instanceof Join || expr instanceof Filter || expr instanceof StatementPattern)
				continue;
			else
				valid = false;
		}
		
		// then process all join arguments but store each valid child BGPs if this join is not a valid BGP
		for (TupleExpr expr : joinArgs) {
			expr.visit(this);
			if (!valid && (expr instanceof Join || expr instanceof Filter || expr instanceof StatementPattern))
				saveBGP(expr, join);
		}
		
		// check if the parent node is a valid BGP node (Join or Filter)
		// because invalid BGP (parent) nodes are not visited
		QueryModelNode parent = join.getParentNode();
		if (valid && !(parent instanceof Join) && !(parent instanceof Filter)) {
			saveBGP(join, parent);
		}
	}
	
}
