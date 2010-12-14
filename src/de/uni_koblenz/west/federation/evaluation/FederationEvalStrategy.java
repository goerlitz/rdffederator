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
package de.uni_koblenz.west.federation.evaluation;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.DistinctIteration;
import info.aduna.iteration.EmptyIteration;
import info.aduna.iteration.UnionIteration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//import org.openrdf.cursor.Cursor;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.TripleSource;
//import org.openrdf.query.algebra.evaluation.cursors.DistinctCursor;
//import org.openrdf.query.algebra.evaluation.cursors.UnionCursor;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.repository.Repository;
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.adapter.SesameAdapter;
import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.federation.helpers.SparqlPrinter;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;
import de.uni_koblenz.west.statistics.RDFStatistics;

/**
 * Implementation of the evaluation strategy for querying distributed data
 * sources. This strategy executes all operators in parallel.
 * 
 * A {@link SourceFinder} is used to provide connections to suitable remote
 * repositories. Sesame's {@link TripleSource}s are not applicable, since
 * they only allow for matching single statement patterns. Hence, a dummy
 * {@link TripleSource} is provided to the {@link EvaluationStrategyImpl}
 * which demands it in the constructor (in order to have access to the
 * {@link ValueFactory}). 
 * 
 * @author Olaf Goerlitz
 */
public class FederationEvalStrategy extends EvaluationStrategyImpl {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FederationEvalStrategy.class);
	
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	
	private static final boolean MULTI_THREADED = true;
	private static final boolean COLLECT_BGP_PATTERNS = true;
	
	private static final SesameAdapter adapter = new SesameAdapter();
	private RDFStatistics stats;
	
	SourceFinder<StatementPattern> finder;
	Map<StatementPattern, Set<Graph>> graphMap;
	
	public FederationEvalStrategy(RDFStatistics stats, final ValueFactory vf) {
		// use a dummy triple source
		// it can handle only single triple patterns but no basic graph patterns
		super(new TripleSource() {
			@Override public ValueFactory getValueFactory() {
				return vf;
			}
//			@Override public Cursor<? extends Statement> getStatements(
//					Resource subj, URI pred, Value obj, Resource... contexts) throws StoreException {
			@Override public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
					Resource subj, URI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
				throw new UnsupportedOperationException("Statement retrival is not supported in federation");
			}
		});
		this.stats = stats;
	}
	
	// -------------------------------------------------------------------------

	/**
	 * Evaluates the join with the specified set of variable bindings as input.
	 * 
	 * @param join
	 *        The Join to evaluate
	 * @param bindings
	 *        The variables bindings to use for evaluating the expression, if
	 *        applicable.
	 * @return A cursor over the variable binding sets that match the join.
	 */
	@Override
//	public Cursor<BindingSet> evaluate(Join join, BindingSet bindings) throws StoreException {
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			Join join, BindingSet bindings) throws QueryEvaluationException {
		
		// eval query if all sub operators are applied on same source
		// TODO optimize with caching
		Set<Graph> sources = new SourceCollector().getSources(join);
		if (COLLECT_BGP_PATTERNS && sources.size() == 1)
			return sendSparqlQuery(join, sources, bindings);
	
//		assert join.getNumberOfArguments() > 0;
		
		// TODO: support different join strategies

		Set<String> resultVars = null;
//		Cursor<BindingSet> joinCursor = null;
		CloseableIteration<BindingSet, QueryEvaluationException> joinCursor = null;
		
//		List<? extends TupleExpr> joinArgs = join.getArgs();
		TupleExpr[] joinArgs = {join.getLeftArg(), join.getRightArg()};
		
		for (TupleExpr joinArg : joinArgs) {
			
//			Cursor<BindingSet> argCursor;
			CloseableIteration<BindingSet, QueryEvaluationException> argCursor;
//			if (MULTI_THREADED) {
//				argCursor = fetchArgResults(joinArg, bindings); 
//			} else {
				argCursor = evaluate(joinArg, bindings);
//			}
			
			
			// init binding names if this is the first argument for the join
			if (joinCursor == null) {
				joinCursor = argCursor;
				resultVars = joinArg.getBindingNames();
				// TODO: can constants vars be removed here?
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("pattern bindings: " + resultVars);
				continue;
			}
			
			// else create hash join (with left and right cursor and join vars)
			Set<String> joinVars = new HashSet<String>(resultVars);
			joinVars.retainAll(joinArg.getBindingNames());
			
			// check for B-Node joins
			for (String varName : joinVars) {
				if (varName.startsWith("-anon"))
					throw new UnsupportedOperationException("blank node joins are not supported");
			}
			
//			if (joinVars.size() == 0) {
//				for (TupleExpr arg : join.getArgs()) {
//					LOGGER.info("cross-prod ARG: " + OperatorTreePrinter.print(arg));					
//				}
//			}
//			
			joinCursor = new HashJoinCursor(joinCursor, argCursor, joinVars);
			resultVars.addAll(joinArg.getBindingNames());

			// TODO: can constants vars be removed here?
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("argument bindings: " + joinVars + "; join bindings: " + resultVars);
		}

		return joinCursor;
	}

	/**
	 * Evaluates the statement pattern against the supplied sources with the
	 * specified set of variable bindings as input.
	 * 
	 * @param sp
	 *        The Statement Pattern to evaluate
	 * @param bindings
	 *        The variables bindings to use for evaluating the expression, if
	 *        applicable.
	 * @return A cursor over the variable binding sets that match the statement
	 *         pattern.
	 */
	@Override
//	public Cursor<BindingSet> evaluate(StatementPattern sp, BindingSet bindings) throws StoreException {
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			StatementPattern sp, BindingSet bindings) throws QueryEvaluationException {
		if (stats == null)
			throw new IllegalArgumentException("need statistics for pattern sources");
		
		String[] values = adapter.getPatternConstants(sp);
		Set<Graph> sources = stats.findSources(values[0], values[1], values[2]);
		if (sources.size() == 0) {
			LOGGER.warn("Cannot find any source for: " + OperatorTreePrinter.print(sp));
//			return EmptyCursor.getInstance();
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		}
		
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("EVAL PATTERN {" + OperatorTreePrinter.print(sp) + "} on sources " + sources);
		return sendSparqlQuery(sp, sources , bindings);
	}
	
	private TupleExpr currentQuery = null;
	
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
		
		synchronized (this) {
		if (currentQuery == null) {
			currentQuery = expr;

			if (finder == null)
				finder = new SourceFinder<StatementPattern>(stats, adapter);
			
			graphMap = new HashMap<StatementPattern, Set<Graph>>();
			Set<StatementPattern> patterns = PatternCollector.getPattern(expr);
			Map<Set<Graph>, List<StatementPattern>> graphSets = finder.findPlanSetsPerSource(patterns);
			for (Set<Graph> graphSet : graphSets.keySet()) {
				for (StatementPattern pattern : graphSets.get(graphSet)) {
					graphMap.put(pattern, graphSet);
				}
			}
		}
		}
		
		
		try {
			return super.evaluate(expr, bindings);
		} finally {
			synchronized (this) {
			if (currentQuery == expr)
				currentQuery = null;
			}
		}
	}
	
	// -------------------------------------------------------------------------
	
//	private Cursor<BindingSet> sendSparqlQuery(TupleExpr expr, Collection<Repository> sources, BindingSet bindings) {
	private CloseableIteration<BindingSet, QueryEvaluationException> sendSparqlQuery(TupleExpr expr, Set<Graph> sources, BindingSet bindings) {
		
		// TODO: need to know actual projection and join variables to reduce transmitted data
		
//		Cursor<BindingSet> cursor;
		CloseableIteration<BindingSet, QueryEvaluationException> cursor;
//		List<Cursor<BindingSet>> cursors = new ArrayList<Cursor<BindingSet>>(sources.size());
		List<CloseableIteration<BindingSet, QueryEvaluationException>> cursors = new ArrayList<CloseableIteration<BindingSet, QueryEvaluationException>>(sources.size());
		final String query = "SELECT DISTINCT * WHERE {" + SparqlPrinter.print(expr) + "}";
		
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Sending SPARQL query to '" + sources + " with bindings " + bindings + "\n" + query);
		
//		for (final Repository rep : sources) {
		for (final Graph rep : sources) {
			if (MULTI_THREADED)
				cursors.add(getMultiThread(rep, query));
			else
//				cursors.add(QueryExecutor.evalQuery(rep, query));
				cursors.add(QueryExecutor.eval(rep.toString(), query));
		}

		// create union if multiple sources are involved
		if (cursors.size() > 1) {
//			cursor = new UnionCursor<BindingSet>(cursors);
			cursor = new UnionIteration<BindingSet, QueryEvaluationException>(cursors);
		} else {
			cursor = cursors.get(0);
		}

		// Filter any duplicates
//		cursor = new DistinctCursor<BindingSet>(cursor);
		cursor = new DistinctIteration<BindingSet, QueryEvaluationException>(cursor);

		return cursor;
		
	}
	
//	public Cursor<BindingSet> getMultiThread(final Graph source, final String query) {
	public CloseableIteration<BindingSet, QueryEvaluationException> getMultiThread(final Graph source, final String query) {
//		Callable<Cursor<BindingSet>> callable = new Callable<Cursor<BindingSet>>() {
		Callable<CloseableIteration<BindingSet, QueryEvaluationException>>  callable = new Callable<CloseableIteration<BindingSet, QueryEvaluationException>>() {
//			@Override public Cursor<BindingSet> call() {
			@Override public CloseableIteration<BindingSet, QueryEvaluationException> call() {
//				return QueryExecutor.evalQuery(repository, query);
				return QueryExecutor.eval(source.toString(), query);
			}
		};
//		Future<Cursor<BindingSet>> future = executor.submit(callable);
		Future<CloseableIteration<BindingSet, QueryEvaluationException>> future = executor.submit(callable);
		return new AsyncCursor<BindingSet>(future);
	}	
	
//	public Cursor<BindingSet> fetchArgResults(final TupleExpr joinArg, final BindingSet bindings) {
	public CloseableIteration<BindingSet, QueryEvaluationException>  fetchArgResults(final TupleExpr joinArg, final BindingSet bindings) {
//		Callable<Cursor<BindingSet>> callable = new Callable<Cursor<BindingSet>>() {
		Callable<CloseableIteration<BindingSet, QueryEvaluationException>>  callable = new Callable<CloseableIteration<BindingSet, QueryEvaluationException>>() {
//			@Override public Cursor<BindingSet> call() {
			@Override public CloseableIteration<BindingSet, QueryEvaluationException> call() {
//				return evaluate(joinArg, bindings);
				try {
					return evaluate(joinArg, bindings);
				} catch (QueryEvaluationException e) {
					e.printStackTrace();
					return null;
				}
			}
		};
//		Future<Cursor<BindingSet>> future = executor.submit(callable);
		Future<CloseableIteration<BindingSet, QueryEvaluationException>> future = executor.submit(callable);
		return new AsyncCursor<BindingSet>(future);
	}
	
	class SourceCollector extends QueryModelVisitorBase<RuntimeException> {
		
		Set<Graph> sources = new HashSet<Graph>();
		
		public Set<Graph> getSources(QueryModelNode node) {
			synchronized (this) {
				sources.clear();
				node.visit(this);
				return sources;
			}
		}

		@Override
		public void meet(StatementPattern pattern) throws RuntimeException {
			sources.addAll(graphMap.get(pattern));
//			String[] values = adapter.getPatternConstants(pattern);
//			sources.addAll(stats.findSources(values[0], values[1], values[2]));
		}
		
	}
	
	static class PatternCollector extends QueryModelVisitorBase<RuntimeException> {
		
		Set<StatementPattern> patternSet = new HashSet<StatementPattern>();
		
		public static Set<StatementPattern> getPattern(QueryModelNode node) {
			PatternCollector collector = new PatternCollector();
			node.visit(collector);
			return collector.patternSet;
		}

		@Override
		public void meet(StatementPattern pattern) throws RuntimeException {
			this.patternSet.add(pattern);
		}		
	}

}
