package de.uni_koblenz.west.federation.test.eval;

import java.io.IOException;

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.FederationSail;
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
	
}
