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

//import org.openrdf.query.algebra.NaryTupleOperator;
//import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

/**
 * Prints the operator tree of a query model.
 * Uses the {@link QueryModelVisitorBase} to handle arbitrary query model nodes.
 * 
 * @author Olaf Goerlitz.
 */
public class OperatorTreePrinter extends QueryModelVisitorBase<RuntimeException> {
	
	private static final OperatorTreePrinter printer = new OperatorTreePrinter();
	
	private StringBuffer buffer = new StringBuffer();
	private String indent = "";
	
	/**
	 * Prints the operator tree starting with the given query model node.
	 *  
	 * @param root the root node of the query model to print.
	 * @return the string representation of the operator tree.
	 */
	public static String print(QueryModelNode root) {
		synchronized (printer) {
			printer.buffer.setLength(0);
			root.visit(printer);
			return printer.buffer.toString();
		}
	}

	// --------------------------------------------------------------

	// Sesame 3.0
	@Override
//	public void meetNaryTupleOperator(NaryTupleOperator node) throws RuntimeException {
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) throws RuntimeException {
		buffer.append(indent);
		buffer.append(node.getSignature().toUpperCase());
		indent += "  ";
		
		// Sesame 3.0:
//		for (TupleExpr expr : node.getArgs()) {
//			buffer.append("\n");
//			expr.visit(this);
//		}
		// Sesame 2.0:
		buffer.append("\n");
		node.getLeftArg().visit(this);
		buffer.append("\n");
		node.getRightArg().visit(this);
		
		indent = indent.substring(0, indent.length() - 2);
	}
	
	@Override
	public void meet(Filter node) throws RuntimeException {
		buffer.append(indent);
		buffer.append("FILTER (");
		node.getCondition().visit(this);
		buffer.append(")");
		indent += "  ";
		
//		for (TupleExpr expr : node.getArgs()) {
//			buffer.append("\n");
//			expr.visit(this);
//		}
		buffer.append("\n");
		node.getArg().visit(this);
		
		indent = indent.substring(0, indent.length() - 2);
	}
	
	@Override
	public void meet(Compare node) throws RuntimeException {
		node.getLeftArg().visit(this);
		buffer.append(" ").append(node.getOperator().getSymbol()).append(" ");
		node.getRightArg().visit(this);
	}
	
	@Override
	public void meet(Var node) throws RuntimeException {
		if (node.hasValue()) {
			Value value = node.getValue();
			if (value instanceof URI)
				buffer.append("<").append(value).append(">");
			else
				buffer.append(node.getValue());
		} else
			buffer.append("?").append(node.getName());
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		buffer.append(indent);
		for (Var var : node.getVarList()) {
			var.visit(this);
			buffer.append(" ");
		}
	}

}
