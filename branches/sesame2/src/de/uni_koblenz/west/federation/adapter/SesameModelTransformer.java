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

import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;

import de.uni_koblenz.west.optimizer.rdf.util.BGPModelTransformer;

/**
 * A Sesame specific model transformer which maps the elements used in
 * a generic Basic Graph Pattern model to Sesame TupleExpr.
 *  
 * @author Olaf Goerlitz
 */
public class SesameModelTransformer extends BGPModelTransformer<TupleExpr, StatementPattern, ValueExpr> {
	
	@Override
	public TupleExpr mapFilter(ValueExpr filter, TupleExpr expr) {
		return new Filter(expr, filter);
	}

	@Override
	public TupleExpr mapJoin(TupleExpr left, TupleExpr right) {
		return new Join(left, right);
	}

	@Override
	public TupleExpr mapPattern(StatementPattern pattern) {
		return pattern;
	}
	
}
