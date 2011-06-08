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

import java.util.HashSet;
import java.util.Set;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.BGPVisitor;

/**
 * Collects different sources, pattern or filters from an operator tree.
 * 
 * @author Olaf Goerlitz
 *
 * @param <P> the triple pattern type.
 * @param <F> the filter type.
 */
public class BGPCollector<P, F> implements BGPVisitor<P, F> {
	
	private Set<F> filters;
	private Set<P> patterns;
	private Set<Graph> sources;
	
	/**
	 * Collects all filters assigned to an operator and its child operators.
	 * 
	 * @param <P> the triple pattern type.
	 * @param <F> the filter type.
	 * @param root the root operator to start with.
	 * @return the set of assigned filters.
	 */
	public static <P, F> Set<F> getFilters(BGPOperator<P, F> root) {
		BGPCollector<P, F> collector = new BGPCollector<P, F>();
		collector.filters = new HashSet<F>();
		root.accept(collector);
		return collector.filters;
	}
	
	/**
	 * Collects all triple patterns of contained access plan nodes.
	 * 
	 * @param <P> the triple pattern type.
	 * @param <F> the filter type.
	 * @param root the root operator to start with.
	 * @return the Set of contained triple patterns.
	 */
	public static <P, F> Set<P> getPatterns(BGPOperator<P, F> root) {
		BGPCollector<P, F> collector = new BGPCollector<P, F>();
		collector.patterns = new HashSet<P>();
		root.accept(collector);
		return collector.patterns;
	}
	
	/**
	 * Collects all triple patterns of contained access plan nodes.
	 * 
	 * @param <P> the triple pattern type.
	 * @param <F> the filter type.
	 * @param root the root operator to start with.
	 * @return the Set of contained triple patterns.
	 */
	public static <P, F> Set<Graph> getSources(BGPOperator<P, F> root) {
		BGPCollector<P, F> collector = new BGPCollector<P, F>();
		collector.sources = new HashSet<Graph>();
		root.accept(collector);
		return collector.sources;
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public void visit(AccessPlan<P, F> plan) {
		if (this.filters != null)
			this.filters.addAll(plan.getFilters());
		if (this.patterns != null)
			this.patterns.add(plan.getPattern());
		if (this.sources != null)
			this.sources.addAll(plan.getSources());
	}

	@Override
	public void visit(BGPJoin<P, F> join) {
		if (this.filters != null)
			this.filters.addAll(join.getFilters());
		join.getLeft().accept(this);
		join.getRight().accept(this);
	}
}
