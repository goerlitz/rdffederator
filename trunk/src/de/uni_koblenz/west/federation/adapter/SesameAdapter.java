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
package de.uni_koblenz.west.federation.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.VarNameCollector;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.ModelAdapter;
import de.uni_koblenz.west.optimizer.rdf.util.BGPSparqlPrinter;
import de.uni_koblenz.west.statistics.RDFValue;

/**
 * Sesame specific implementation of the model adapter.
 * 
 * @author Olaf Goerlitz
 */
public class SesameAdapter implements ModelAdapter<StatementPattern, ValueExpr> {
	
	private BGPSparqlPrinter<StatementPattern, ValueExpr> printer;
	
	public SesameAdapter() {
		printer = new BGPSparqlPrinter<StatementPattern, ValueExpr>(this);
	}
	
	@Override
	public RDFValue getOBinding(StatementPattern pattern) {
		Value value = pattern.getObjectVar().getValue();
		if (value != null)
			return new SesameRDFValue(value);
		else
			return null;
	}

	@Override
	public URI getPBinding(StatementPattern pattern) {
		Value value = pattern.getPredicateVar().getValue();
		if (value != null)
			try {
				return new URI(value.stringValue());
			} catch (URISyntaxException e) {
				// not possible
				return null;
			}
		else
			return null;
	}

	@Override
	public URI getSBinding(StatementPattern pattern) {
		Value value = pattern.getSubjectVar().getValue();
		if (value != null)
			try {
				return new URI(value.stringValue());
			} catch (URISyntaxException e) {
				// not possible
				return null;
			}
		else
			return null;
	}

	@Override
	public Set<String> getFilterVars(ValueExpr filter) {
		return VarNameCollector.process(filter);
	}

	/**
	 * Returns the pattern constants in an array representing the triple.
	 * @param pattern the pattern to analyze.
	 * @return the pattern constants in an array.
	 */
	@Override
	public String[] getPatternConstants(StatementPattern pattern) {
		String[] constants = new String[3];
		Var var = pattern.getSubjectVar();
		if (var.getValue() != null)
			constants[0] = var.getValue().stringValue();
		var = pattern.getPredicateVar();
		if (var.getValue() != null)
			constants[1] = var.getValue().stringValue();
		var = pattern.getObjectVar();
		if (var.getValue() != null)
			constants[2] = var.getValue().stringValue();
		return constants;
	}

	@Override
	public Set<String> getPatternVars(StatementPattern pattern) {
		return pattern.getBindingNames();
	}
	
	@Override
	public String getVarName(StatementPattern pattern, int triplePos) {
		Var var = null;
		switch (triplePos) {
		case 0:
			var = pattern.getSubjectVar();
			break;
		case 1:
			var = pattern.getPredicateVar();
			break;
		case 2:
			var = pattern.getObjectVar();
			break;
		default: throw new IllegalArgumentException("not a valid triple position: " + triplePos);
		}
		return var.getName();
	}
	
	@Override
	public int getVarPosition(StatementPattern pattern, String varName) {
		if (pattern.getSubjectVar().getName().equals(varName))
			return 1;
		if (pattern.getPredicateVar().getName().equals(varName))
			return 2;
		if (pattern.getObjectVar().getName().equals(varName))
			return 3;
		return -1;
	}
	
	@Override
	public String toSparqlFilter(ValueExpr filter) {
		return OperatorTreePrinter.print(filter);
	}

	@Override
	public String toSparqlPattern(StatementPattern pattern) {
		return OperatorTreePrinter.print(pattern);
	}

	@Override
	public String toSparqlBGP(BGPOperator<StatementPattern, ValueExpr> operator) {
		return printer.eval(operator);
	}
}
