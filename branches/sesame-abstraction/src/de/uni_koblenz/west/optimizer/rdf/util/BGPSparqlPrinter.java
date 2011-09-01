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

import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.ModelAdapter;
import de.uni_koblenz.west.optimizer.rdf.eval.BGPModelEvaluator;

/**
 * Converts a Basic Graph Pattern to its SPARQL representation.
 * The visitor pattern is used to traverse the model tree.
 * 
 * @author Olaf Goerlitz
 *
 * @param <P> the triple pattern type.
 * @param <F> the filter type.
 */
public class BGPSparqlPrinter<P, F> implements BGPModelEvaluator<P, F, String> {
	
	private StringBuffer buffer = new StringBuffer();
	private String indent = "";
	private ModelAdapter<P, F> adapter;
	
	/**
	 * Creates a new SPARQL printer with the appropriate model adapter.
	 * 
	 * @param adapter the model adapter to use.
	 */
	public BGPSparqlPrinter(ModelAdapter<P, F> adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("model adapter must not be null");
		this.adapter = adapter;
	}
	
	/**
	 * Prints the SPARQL query starting with the given operator node.
	 *  
	 * @param root the root node of the query model to print.
	 * @return the SPARQL representation of the query model.
	 */
	@Override
	public String eval(BGPOperator<P, F> root) {
		synchronized (this) {
			buffer.setLength(0);
			root.accept(this);
			return buffer.toString();
		}
	}
	
	@Override
	public void visit(AccessPlan<P, F> plan) {
		
		buffer.append(indent);
		buffer.append(this.adapter.toSparqlPattern(plan.getPattern()));
		buffer.append(".\n");
		
		for (F filter : plan.getFilters()) {
			buffer.append(indent);
			buffer.append(this.adapter.toSparqlFilter(filter));
			buffer.append("\n");
		}
	}

	@Override
	public void visit(BGPJoin<P, F> join) {
		join.getLeft().accept(this);
		join.getRight().accept(this);
		
		for (F filter : join.getFilters()) {
			buffer.append(indent);
			buffer.append("FILTER (");
			buffer.append(this.adapter.toSparqlFilter(filter));
			buffer.append(")\n");
		}
	}

}
