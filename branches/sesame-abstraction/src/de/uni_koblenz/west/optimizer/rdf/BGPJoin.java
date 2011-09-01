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
package de.uni_koblenz.west.optimizer.rdf;

import de.uni_koblenz.west.optimizer.rdf.BGPQueryModel.JoinAlgo;
import de.uni_koblenz.west.optimizer.rdf.BGPQueryModel.JoinExec;

/**
 * Join operator with different join strategies and join algorithms.
 * 
 * @author Olaf Goerlitz
 */
public class BGPJoin<P, F> extends BGPOperator<P, F> {
	
	protected BGPQueryModel.JoinExec exec;
	protected BGPQueryModel.JoinAlgo algo;
	protected BGPOperator<P, F> left;
	protected BGPOperator<P, F> right;
	
	protected BGPJoin(JoinExec exec, JoinAlgo algo, BGPOperator<P, F> left, BGPOperator<P, F> right) {
		this.exec = exec;
		this.algo = algo;
		this.left = left;
		this.right = right;
	}
	
	public BGPOperator<P, F> getLeft() {
		return this.left;
	}
	
	public BGPOperator<P, F> getRight() {
		return this.right;
	}
	
	@Override
	public void accept(BGPVisitor<P, F> visitor) {
		visitor.visit(this);
	}
	
}
