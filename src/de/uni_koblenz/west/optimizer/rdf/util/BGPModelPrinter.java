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

import java.util.Set;

import de.uni_koblenz.west.optimizer.eval.QueryModelEvaluator;
import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.ModelAdapter;
import de.uni_koblenz.west.optimizer.rdf.eval.BGPModelEvaluator;

/**
 * Prints the operator tree of a basic graph pattern model.
 * Inherits from {@link BGPModelEvaluator} to apply the visitor pattern.
 * 
 * @author Olaf Goerlitz
 */
public class BGPModelPrinter<P, F> implements BGPModelEvaluator<P, F, String> {

	private StringBuffer buffer = new StringBuffer();
	private String indent = "";
	private ModelAdapter<P, F> adapter;
	private QueryModelEvaluator<BGPOperator<P, F>, ? extends Number> evaluator;

	/**
	 * Create a new model printer with the appropriate model adapter.
	 * 
	 * @param adapter the model adapter to use.
	 */
	public BGPModelPrinter(ModelAdapter<P, F> adapter) {
		if (adapter == null)
			throw new IllegalArgumentException("model adapter must not be null");
		this.adapter = adapter;
	}

	/**
	 * Set an additional evaluator to include meta data for each model node.
	 * 
	 * @param evaluator the additional evaluator to use.
	 */
	public void setEvaluator(QueryModelEvaluator<BGPOperator<P, F>, ? extends Number> evaluator) {
		this.evaluator = evaluator;
	}

	/**
	 * Prints the operator tree starting with the given operator node.
	 *  
	 * @param root the root node of the query model to print.
	 * @return the string representation of the operator tree.
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
		addFilters(plan.getFilters());

		buffer.append(indent);
		buffer.append("PATTERN ");
		if (this.evaluator != null) {
			buffer.append("[" + this.evaluator.eval(plan) + "] ");
		}

		buffer.append(this.adapter.toSparqlPattern(plan.getPattern()));

		for (int i = plan.getFilters().size(); i != 0; i--)
			indent = indent.substring(0, indent.length() - 2);
	}

	@Override
	public void visit(BGPJoin<P, F> join) {
		addFilters(join.getFilters());
		buffer.append(indent);
		buffer.append("JOIN ");
		if (this.evaluator != null) {
			buffer.append("[" + this.evaluator.eval(join) + "] ");
		}
		buffer.append("\n");
		indent += "  ";
		join.getLeft().accept(this);
		buffer.append("\n");
		join.getRight().accept(this);
		indent = indent.substring(0, indent.length() - 2);
		for (int i = join.getFilters().size(); i != 0; i--)
			indent = indent.substring(0, indent.length() - 2);
	}
	
	// -------------------------------------------------------------------------
	
	private void addFilters(Set<F> filters) {
		for (F filter : filters) {
			buffer.append(indent);
			buffer.append("FILTER (");
			buffer.append(adapter.toSparqlFilter(filter));
			buffer.append(")\n");
			indent += "  ";
		}
	}

}
