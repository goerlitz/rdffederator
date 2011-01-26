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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.FederationSail;
import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.federation.test.config.Configuration;
import de.uni_koblenz.west.federation.test.config.ConfigurationException;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;

/**
 * 
 * @author Olaf Goerlitz
 */
public class SourceFinderTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceFinderTest.class);
	
	private static final String CONFIG_FILE = "setup/life-science-config.prop";
	
	private static Repository REPOSITORY;
	private static Iterator<String> QUERIES;
	private static SourceFinder<StatementPattern> finder;
	
	public static void main(String[] args) {
		
		// check arguments for name of configuration file
		String configFile;
		if (args.length == 0) {
			LOGGER.info("no config file specified; using default: " + CONFIG_FILE);
			configFile = CONFIG_FILE;
		} else {
			configFile = args[0];
		}
		
		// load configuration file and create repository
		setup(configFile);
	}
	
	private static void setup(String configFile) {
		
		try {
			Configuration config = Configuration.create(configFile);
			REPOSITORY = config.createRepository();
			QUERIES = config.getQueryIterator();
			SourceFinder<StatementPattern> finder = config.getSourceFinder();
			System.out.println("finder rdf:type=" + finder.isHandleRDFType());
		} catch (IOException e) {
			LOGGER.error("cannot load test config: " + e.getMessage());
		} catch (ConfigurationException e) {
			LOGGER.error("failed to create repository: " + e.getMessage());
		}
		
		FederationSail sail = ((FederationSail) ((SailRepository) REPOSITORY).getSail());
		finder = sail.getSourceFinder();
	}
	
	// -------------------------------------------------------------------------
	
    @BeforeClass
    public static void setUp() {
    	setup(CONFIG_FILE);
    }
    
	public void testQueries() {
		while (QUERIES.hasNext()) {
			String query = QUERIES.next();
			
			long start = System.currentTimeMillis();
			List<BindingSet> result = QueryExecutor.eval(REPOSITORY, query);
			LOGGER.info("Evaluation time: " + (System.currentTimeMillis() - start));
			LOGGER.info("RESULT SIZE: " + (result != null ? result.size() : -1));
		}
	}

}