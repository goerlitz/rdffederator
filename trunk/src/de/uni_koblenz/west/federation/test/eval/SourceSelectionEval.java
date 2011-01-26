package de.uni_koblenz.west.federation.test.eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.federation.test.config.Configuration;
import de.uni_koblenz.west.federation.test.config.ConfigurationException;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;

/**
 * Evaluation of the source selection.
 * 
 * @author goerlitz@uni-koblenz.de
 */
public class SourceSelectionEval {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceSelectionEval.class);
	
	private static final String CONFIG_FILE = "setup/fed-test.properties";
	
	private SourceFinder<StatementPattern> finder;
	private Iterator<String> queries;
	
	public SourceSelectionEval(Configuration config) throws ConfigurationException {
		this.queries = config.getQueryIterator();
		this.finder = config.getSourceFinder();
	}
	
	public void testQueries() {
		List<Integer> results = new ArrayList<Integer>();
		
//		while (this.queries.hasNext()) {
		for (int i=0; this.queries.hasNext(); i++) {
			String query = this.queries.next();
			SPARQLParser parser = new SPARQLParser();
			TupleExpr expr;
			try {
				expr = parser.parseQuery(query, null).getTupleExpr();
			} catch (MalformedQueryException e) {
				LOGGER.error("cannot parse Query");
				continue;
			}
			List<StatementPattern> patterns = StatementPatternCollector.process(expr);
			Map<Set<Graph>, List<StatementPattern>> sourceMap = this.finder.findPlanSetsPerSource(patterns);
			
			// evaluation
			Set<Graph> selectedSources = new HashSet<Graph>();
			int queriesToSend = 0;
			int patternToSend = 0;
			for (Set<Graph> sourceSet : sourceMap.keySet()) {
				selectedSources.addAll(sourceSet);
				int patternCount = sourceMap.get(sourceSet).size();
				queriesToSend += sourceSet.size();
				patternToSend += sourceSet.size() * patternCount;
			}
			
			results.add(selectedSources.size());
//			System.out.println(i + "\t" + selectedSources.size());
//			System.out.println(selectedSources.size() + " sources selected: " + selectedSources);
//			System.out.println(queriesToSend + " queries, " + patternToSend + " patterns");
		}
		
		System.out.println("result: " + results);
	}
	
	public static void main(String[] args) {
		
		// check arguments for name of configuration file
		String configFile;
		if (args.length == 0) {
			LOGGER.info("no config file specified; using default: " + CONFIG_FILE);
			configFile = CONFIG_FILE;
		} else {
			configFile = args[0];
		}
		
		try {
			Configuration config = Configuration.create(configFile);
			SourceSelectionEval eval = new SourceSelectionEval(config);
			eval.testQueries();
		} catch (IOException e) {
			LOGGER.error("cannot load test config: " + e.getMessage());
		} catch (ConfigurationException e) {
			LOGGER.error("cannot create repository: " + e.getMessage());
		}
		
	}
	
}