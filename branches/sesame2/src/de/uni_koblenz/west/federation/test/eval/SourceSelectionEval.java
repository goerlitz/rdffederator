package de.uni_koblenz.west.federation.test.eval;

import java.io.IOException;
import java.io.PrintStream;
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

import de.uni_koblenz.west.federation.adapter.SesameAdapter;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.federation.sources.SourceSelector;
import de.uni_koblenz.west.federation.test.config.Configuration;
import de.uni_koblenz.west.federation.test.config.ConfigurationException;
import de.uni_koblenz.west.federation.test.config.Query;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;

/**
 * Evaluation of the source selection.
 * 
 * @author goerlitz@uni-koblenz.de
 */
public class SourceSelectionEval {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceSelectionEval.class);
	
	private static final String CONFIG_FILE = "setup/fed-test.properties";
	
//	private SourceFinder<StatementPattern> finder;
	private SourceSelector<StatementPattern> finder;
	private Iterator<Query> queries;
	private PrintStream output;
	
	public SourceSelectionEval(Configuration config) throws ConfigurationException {
		this.queries = config.getQueryIterator();
//		this.finder = config.getSourceFinder();
		this.finder = config.getSourceSelector();
		this.output = config.getResultStream();
	}
	
	public void testQueries() {
		
		// table header
		output.println("#query\tsources\treqSent\tpatSent");
		
		while (this.queries.hasNext()) {
			Query query = this.queries.next();
			SPARQLParser parser = new SPARQLParser();
			TupleExpr expr;
			try {
				expr = parser.parseQuery(query.getQuery(), null).getTupleExpr();
			} catch (MalformedQueryException e) {
				LOGGER.error("cannot parse Query " + query.getName() + ": " + e.getMessage());
				continue;
			}
			List<StatementPattern> patterns = StatementPatternCollector.process(expr);
//			Map<Set<Graph>, List<StatementPattern>> sourceMap = this.finder.findPlanSetsPerSource(patterns);
			Map<Set<Graph>, List<StatementPattern>> sourceMap = this.finder.getSources(patterns);
			
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
			
			// print results
			for (Set<Graph> key : sourceMap.keySet()) {
				List<StatementPattern> patternList = sourceMap.get(key);
				List<String> pStrings = new ArrayList<String>();
				for (StatementPattern p : patternList) {
					pStrings.add(new SesameAdapter().toSparqlPattern(p));
				}
				System.out.println(key + " -> " + pStrings);
			}
			
			output.println(query.getName() + "\t" + selectedSources.size() + "\t" + queriesToSend + "\t" + patternToSend);
		}
		output.close();
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
			Configuration config = Configuration.load(configFile);
			SourceSelectionEval eval = new SourceSelectionEval(config);
			eval.testQueries();
		} catch (IOException e) {
			LOGGER.error("cannot load test config: " + e.getMessage());
		} catch (ConfigurationException e) {
			LOGGER.error("cannot init configuration: " + e.getMessage());
		}
		
	}
	
}
