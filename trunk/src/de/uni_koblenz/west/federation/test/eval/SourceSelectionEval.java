package de.uni_koblenz.west.federation.test.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.FederationSail;
import de.uni_koblenz.west.federation.helpers.Configuration;
import de.uni_koblenz.west.federation.helpers.ConfigurationException;
import de.uni_koblenz.west.federation.helpers.ConfigurationManager;
import de.uni_koblenz.west.federation.test.FederationRepositoryTest;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;

/**
 * Evaluation of the source selection.
 * 
 * @author goerlitz@uni-koblenz.de
 */
public class SourceSelectionEval {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceSelectionEval.class);
	
	private static final String CONFIG_FILE = "setup/fed-test.properties";
	private static final String PROP_QUERY_DIR  = "query.directory";
	private static final String PROP_QUERY_EXT  = "query.extension";
	
	private static Repository REPOSITORY;
	private static SourceFinder<StatementPattern> finder;
	
	public static void main(String[] args) {
		
		// check arguments for name of configuration file
		String configFile;
		if (args.length == 0) {
			LOGGER.info("missing test config file; using default: " + CONFIG_FILE);
			configFile = CONFIG_FILE;
		} else {
			configFile = args[0];
		}
		
		try {
			Configuration config = Configuration.create(configFile);
			Repository rep = config.createRepository();
			
			FederationSail sail = ((FederationSail) ((SailRepository) rep).getSail());
			finder = sail.getSourceFinder();
			
		} catch (IOException e) {
			LOGGER.error("cannot load test config: " + e.getMessage());
		} catch (ConfigurationException e) {
			LOGGER.error("cannot create repository: " + e.getMessage());
		}
		
	}
	
    /**
     * Locate all queries to use for evaluation.
     * 
     * @param props the properties containing the location of the queries.
     * @return a list of query files.
     */
    private static List<File> findQueries(Properties props) {
    	
    	// get query directory and extension or assume current dir and '.sparql'
		final String queryDir = props.getProperty(PROP_QUERY_DIR, ".");
		final String queryExt = props.getProperty(PROP_QUERY_EXT, ".sparql");
		
		if (props.getProperty(PROP_QUERY_DIR) == null)
			LOGGER.info("property '" + PROP_QUERY_DIR + "' not specified: using '" + queryDir + "'");
		if (props.getProperty(PROP_QUERY_EXT) == null)
			LOGGER.info("property '" + PROP_QUERY_EXT + "' not specified: using '" + queryExt + "'");

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
	 * 
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
	
//	private static void setup(Properties config) {
//		REPOSITORY = createRepository(config);
//		
//		FederationSail sail = ((FederationSail) ((SailRepository) REPOSITORY).getSail());
//		finder = sail.getSourceFinder();
//	}

}
