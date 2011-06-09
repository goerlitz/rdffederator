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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.ValueExpr;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.rdf.AccessPlan;
import de.uni_koblenz.west.optimizer.rdf.BGPJoin;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.BGPQueryModel;
import de.uni_koblenz.west.optimizer.rdf.eval.QueryModelVerifier;
import de.uni_koblenz.west.optimizer.rdf.util.BGPModelPrinter;

/**
 * Sesame specific realization of the basic graph pattern model.
 * 
 * @author Olaf Goerlitz
 */
public class SesameBGPWrapper extends BGPQueryModel<StatementPattern, ValueExpr> {

	private static final BGPModelPrinter<StatementPattern, ValueExpr> printer
		= new BGPModelPrinter<StatementPattern, ValueExpr>(new SesameAdapter());
	
	protected Set<StatementPattern> patterns = new HashSet<StatementPattern>();
	protected Set<ValueExpr> conditions = new HashSet<ValueExpr>();
	
	protected QueryModelVerifier<StatementPattern, ValueExpr> listener;
	protected QueryModelNode root;
	protected QueryModelNode parent;
	
	public SesameBGPWrapper(QueryModelNode root, QueryModelNode rootParent, List<StatementPattern> patterns, List<ValueExpr> filters) {
		super(new SesameAdapter());
		this.patterns.addAll(patterns);
		this.conditions.addAll(filters);
		
		this.root = root;
		this.parent = rootParent;
	}
	
	public void setListener(QueryModelVerifier<StatementPattern, ValueExpr> verifier) {
		this.listener = verifier;
	}
	
	// --- OVERRIDE ------------------------------------------------------------
	
	protected void createBaseRelations() {
		if (sourceFinder == null)
			throw new IllegalArgumentException("source finder must not be null");
		
		baseOperators = new ArrayList<BGPOperator<StatementPattern, ValueExpr>>();
//		Map<Set<Graph>, List<P>> graphSets = sourceFinder.findPlanSetsPerSource(getAllPatterns());
		Map<Set<Graph>, List<StatementPattern>> graphSets = sourceFinder.getSources(getAllPatterns());
		
		for (Set<Graph> graphSet : graphSets.keySet()) {
			List<StatementPattern> plans = graphSets.get(graphSet);
			// combine all operators for one source
			if (graphSet.size() == 1) {
				baseOperators.add(joinPatterns(plans, graphSet));				
			} else {
				// or create separate operators for each source
				for (StatementPattern pattern : plans) {
					baseOperators.add(createPlan(pattern, graphSet));
				}
			}
		}
	}
	
	/**
	 * Sesame specific class providing string representation for DEBUGGING.
	 */
	@Override
	protected AccessPlan<StatementPattern, ValueExpr> createPlan(
			StatementPattern pattern, Set<Graph> sources) {
		return new AccessPlan<StatementPattern, ValueExpr>(pattern, sources) {
			@Override
			public String toString() {
				return printer.eval(this);
			}
		};
	}

	/**
	 * Sesame specific class providing string representation for DEBUGGING.
	 */
	@Override
	protected BGPJoin<StatementPattern, ValueExpr> createJoin(JoinExec exec, JoinAlgo algo, BGPOperator<StatementPattern, ValueExpr> left, BGPOperator<StatementPattern, ValueExpr> right) {
		return new BGPJoin<StatementPattern, ValueExpr>(exec, algo, left, right) {
			@Override
			public String toString() {
				return printer.eval(this);
			}
		};
	}

	@Override
	protected Set<ValueExpr> getAllFilters() {
		return this.conditions;
	}

	@Override
	protected Set<StatementPattern> getAllPatterns() {
		return this.patterns;
	}
	
	@Override
	public void replaceRoot(BGPOperator<StatementPattern, ValueExpr> operator) {
//		System.out.println("transform model:\n" + new SesameAdapter().toSparqlBGP(operator));
		TupleExpr newRoot = new SesameModelTransformer().eval(operator);
//		System.out.println("changed model:\n" + OperatorTreePrinter.print(newRoot));
		
		this.parent.replaceChildNode(root, newRoot);
		this.root = newRoot;
		
		// verify the optimization result
		if (this.listener != null) {
			this.listener.resultObtained(this.listener.getEvaluator().eval(operator));
		}
	}

	@Override
	public String toString() {
		return OperatorTreePrinter.print(this.root);
	}
	
}
