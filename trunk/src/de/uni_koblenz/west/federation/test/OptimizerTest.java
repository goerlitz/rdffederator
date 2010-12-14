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
package de.uni_koblenz.west.federation.test;

import static de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType.STATISTICS;
import static de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType.TRUE_COUNT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
//import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.FederationOptimizer;
import de.uni_koblenz.west.federation.FederationOptimizerFactory;
import de.uni_koblenz.west.federation.helpers.Format;
import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType;
import de.uni_koblenz.west.optimizer.eval.CostCalculator;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.eval.QueryModelEvaluator;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.eval.QueryModelVerifier;
import de.uni_koblenz.west.statistics.Void2StatsRepository;

/**
 * Testing the query optimization
 * 
 * @author Olaf Goerlitz
 */
public class OptimizerTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OptimizerTest.class);
	
	private static final String SP2B_CONFIG = "sp2b/sp2b_50k_void2.n3";
	private static final String QUERY_DIR = "sp2b";
	private static final String QUERY_EXT = ".sparql";
//	private static final String SP2B_CONFIG = "federated/RDFFederator_config.n3";
//	private static final String QUERY_DIR = "federated/cd";
//	private static final String QUERY_EXT = ".txt";
	
	private static final Void2StatsRepository STATS = new Void2StatsRepository();
	private static final SPARQLParser PARSER = new SPARQLParser();
	
	public static void main(String[] args) {
		setUp();
		new OptimizerTest().testOptimizer();
	}
	
    @BeforeClass
    public static void setUp() {
		try {
			STATS.load(OptimizerTest.class.getResource(SP2B_CONFIG));
		} catch (IOException e) {
			LOGGER.error("can not read voiD description: " + e.getMessage());
			throw new RuntimeException("can not read voiD description: " + e.getMessage(), e);
		}
    }
    
	@Test
	public void testOptimizer() {
		
		// initialize optimizer
		CostModel costModel = new CostModel();
		FederationOptimizerFactory factory = new FederationOptimizerFactory();
		factory.setStatistics(STATS);
		factory.setCostmodel(costModel);
		
//		FederationOptimizer phOptimizer = factory.getOptimizer("PATTERN_HEURISTIC", STATISTICS.toString());
		FederationOptimizer phOptimizer = factory.getOptimizer("DYNAMIC_PROGRAMMING", STATISTICS.toString());
		
		final CostCalculator<BGPOperator<StatementPattern, ValueExpr>> postEval = factory.getCostCalculator(STATISTICS, costModel);
//		final CostCalculator<BGPOperator<StatementPattern, ValueExpr>> postEval = factory.getCostCalculator(TRUE_COUNT, costModel);
//		final QueryModelEvaluator<BGPOperator<StatementPattern, ValueExpr>, ? extends Number> postEval = factory.getEstimator(TRUE_COUNT);
		
		
		QueryModelVerifier<StatementPattern, ValueExpr> verifier = new QueryModelVerifier<StatementPattern, ValueExpr>() {
			@Override
			public QueryModelEvaluator<BGPOperator<StatementPattern, ValueExpr>, ? extends Number> getEvaluator() {
				return postEval;
			}
			@Override
			public void resultObtained(Number value) {
				System.out.println(Format.d(value.doubleValue(), 2));
			}
		};
		
//		phOptimizer.setResultVerifier(verifier);
		phOptimizer.setEvaluator(postEval);
//		dpOptimizer.setResultVerifier(verifier);
		
		for (File filename : findQueries(QUERY_DIR, QUERY_EXT)) {
			String query = readQuery(filename);
//			QueryModel model = PARSER.parseQuery(query, null);
			
			// Sesame 2:
			ParsedQuery model = null;
			try {
				model = PARSER.parseQuery(query, null);
			} catch (MalformedQueryException e) {
				e.printStackTrace();
				continue;
			}
			
			if (LOGGER.isInfoEnabled())
//				LOGGER.info(filename.getName() + ":\n" + OperatorTreePrinter.print(model));
				LOGGER.info(filename.getName() + ":\n" + OperatorTreePrinter.print(model.getTupleExpr()));
			
			// Sesame 3:
//			try {
//				phOptimizer.optimize(model, EmptyBindingSet.getInstance());
//				dpOptimizer.optimize(model, EmptyBindingSet.getInstance());
//			} catch (StoreException e) {
//				e.printStackTrace();
//			}
			
			// Sesame 2:
			phOptimizer.optimize(model.getTupleExpr(), model.getDataset(), EmptyBindingSet.getInstance());
//			dpOptimizer.optimize(model.getTupleExpr(), model.getDataset(), EmptyBindingSet.getInstance());
			
		}
	}
	
	// -------------------------------------------------------------------------
	
    private List<File> findQueries(String queryDir, String queryExt) {
    	
		// TODO: this does not work for absolute path.
		URL url = FederationRepositoryTest.class.getResource(queryDir);
		if (url == null) {
			LOGGER.error("cannot find query directory: " + queryDir);
			throw new RuntimeException("cannot find query directory: " + queryDir);
		}
		File dir = new File(url.getPath());
		if (!dir.isDirectory())
			throw new IllegalArgumentException("not a directory: " + dir);
		
		List<File> queries = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (file.isFile() && file.getName().endsWith(queryExt)) {
				queries.add(file);
			}
		}
		
		Collections.sort(queries);
		return queries;
    }
    
	/**
	 * Read a query from a file.
	 * @param file the file to read.
	 * @return the query.
	 */
	private String readQuery(File query) {
		StringBuffer buffer = new StringBuffer();
		try {
			BufferedReader r = new BufferedReader(new FileReader(query));
			String input;
			while((input = r.readLine()) != null) {
				buffer.append(input).append("\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return buffer.toString();
	}
	
}
