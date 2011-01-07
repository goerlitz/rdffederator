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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.federation.helpers.RepositoryCreator;

/**
 * Test Federation repository which is based on the FederationSail.
 * The federation settings are loaded from a local configuration file.
 * 
 * @author Olaf Goerlitz
 */
public class FederationRepositoryTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FederationRepositoryTest.class);
	
//	private static final String CONFIG     = "test-config.prop";
//	private static final String CONFIG     = "federation-config.prop";
	private static final String CONFIG     = "config/life-science-config.prop";
	
	private static final String REP_CONFIG = "repository.config";
	private static final String QUERY_DIR  = "query.directory";
	private static final String QUERY_EXT  = "query.extension";
//	private static final String STRATEGY   = "optimizer.strategy";
//	private static final String ESTIMATOR  = "optimizer.estimator";
	
	private static Repository REPOSITORY;
	private static Properties PROPS;
	
	private String query;
	
	public static void main(String[] args) {
		setUp();
		new FederationRepositoryTest().testQueries();
	}
	
    @BeforeClass
    public static void setUp() {
    	PROPS = new Properties();
    	try {
    		PROPS.load(FederationRepositoryTest.class.getResourceAsStream(CONFIG));
    		LOGGER.info("loaded configuration from: " + CONFIG);
    	} catch (IOException e) {
    		LOGGER.error("can not read config file: " + CONFIG);
    		throw new RuntimeException("can not read config file: " + CONFIG);
    	}
		
		String repConfig = PROPS.getProperty(REP_CONFIG);
		URL configURL = FederationRepositoryTest.class.getResource(repConfig);
		REPOSITORY = new RepositoryCreator().load(configURL);
    }
    
//	@Test
	public void testPatternQueries() {
		
		query = "SELECT DISTINCT * WHERE { [] a ?type }";
		QueryExecutor.eval(REPOSITORY, query);
		
		query = "SELECT DISTINCT * WHERE { ?x a [] }";
		QueryExecutor.eval(REPOSITORY, query);
		
		// MUST FAIL: unbound predicate
		query = "SELECT DISTINCT * WHERE { [] ?p [] }";
//		try {
			QueryExecutor.eval(REPOSITORY, query);
//			Assert.fail("Should have raised an UnsupportedOperationException");
//		} catch (UnsupportedOperationException e) {
//		}

		// MUST FAIL: unbound predicate
		query = "SELECT DISTINCT ?p WHERE { ?x a ?type; ?p [] }";
		try {
			QueryExecutor.eval(REPOSITORY, query);
			Assert.fail("Should have raised an UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
		}
		
		// MUST FAIL: join over blank nodes is not supported
		query = "SELECT DISTINCT * WHERE { [] a ?type; ?p [] }";
		try {
			QueryExecutor.eval(REPOSITORY, query);
			Assert.fail("Should have raised an UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
		}
	}
	
	@Test
	public void testQueries() {
		for (File filename : findQueries(PROPS)) {
			String query = readQuery(filename);
			
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(filename.getName() + ":\n" + query);
			else
				LOGGER.info(filename.getName());
			
			long start = System.currentTimeMillis();
			List<BindingSet> result = QueryExecutor.eval(REPOSITORY, query);
			LOGGER.info("Evaluation time: " + (System.currentTimeMillis() - start));
			LOGGER.info("RESULT SIZE: " + (result != null ? result.size() : -1));
		}
	}
	
	// -------------------------------------------------------------------------
	
    /**
     * Locate all queries to use for evaluation.
     * 
     * @param props the properties containing the location of the queries.
     * @return a list of query files.
     */
    private List<File> findQueries(Properties props) {
    	
    	// get query directory and extension or assume current dir and '.sparql'
		final String queryDir = props.getProperty(QUERY_DIR, ".");
		final String queryExt = props.getProperty(QUERY_EXT, ".sparql");
		
		if (props.getProperty(QUERY_DIR) == null)
			LOGGER.info("property '" + QUERY_DIR + "' not specified: using '" + queryDir + "'");
		if (props.getProperty(QUERY_EXT) == null)
			LOGGER.info("property '" + QUERY_EXT + "' not specified: using '" + queryExt + "'");

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
    
}
