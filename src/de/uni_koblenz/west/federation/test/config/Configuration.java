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
package de.uni_koblenz.west.federation.test.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.openrdf.model.Graph;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryRegistry;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration object for test scenarios.
 * 
 * @author Olaf Goerlitz
 */
public class Configuration {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	
	private static final String PROP_REP_CONFIG = "repository.config";
	private static final String PROP_QUERY_DIR  = "query.directory";
	private static final String PROP_QUERY_EXT  = "query.extension";
	
	private File cfgFile;
	private Properties props = new Properties();
	
	private Configuration(String fileName) throws IOException {
		this.cfgFile = new File(fileName).getAbsoluteFile();
		this.props.load(new FileReader(this.cfgFile));
		LOGGER.info("loaded config properties from " + cfgFile);
	}
	
	public static Configuration create(String configFile) throws IOException {
		return new Configuration(configFile);
	}
	
	public Repository createRepository() throws ConfigurationException {
		
		// get repository config file
		String repConfig = props.getProperty(PROP_REP_CONFIG);
		if (repConfig == null) {
			throw new ConfigurationException("missing config file setting '" + PROP_REP_CONFIG + "' in " + cfgFile);
		}
		
		// create repository
    	try {
    		// using configuration directory as base for resolving relative URIs
			RepositoryConfig repConf = RepositoryConfig.create(loadRDFConfig(cfgFile.toURI().resolve(repConfig), cfgFile.toURI().toString()), null);
			repConf.validate();
			RepositoryImplConfig implConf = repConf.getRepositoryImplConfig();
			RepositoryRegistry registry = RepositoryRegistry.getInstance();
			RepositoryFactory factory = registry.get(implConf.getType());
			if (factory == null) {
				throw new ConfigurationException("Unsupported repository type: " + implConf.getType() + " in repository config");
			}
			Repository repository = factory.getRepository(implConf);
			repository.initialize();
			return repository;
		} catch (RepositoryConfigException e) {
			throw new ConfigurationException("cannot create repository: " + e.getMessage());
		} catch (RepositoryException e) {
			throw new ConfigurationException("cannot initialize repository: " + e.getMessage());
		}
	}
	
	public Iterator<String> getQueryIterator() throws ConfigurationException {
		
		return new Iterator<String>() {
			
			private List<File> files = getQueryList();

			@Override
			public boolean hasNext() {
				return files.size() != 0;
			}

			@Override
			public String next() {
				if (files.size() == 0)
					throw new IllegalStateException("no more query files");
				
				File file = files.remove(0);
				try {
					String query = readQuery(file);
					
					if (LOGGER.isDebugEnabled())
						LOGGER.debug(file + ":\n" + query);
					else
						LOGGER.info(file.toString());
					
					return query;
				} catch (IOException e) {
					throw new RuntimeException("can not load query " + file, e);
				}
			}

			@Override
			public void remove() {}
		};
	}
	
	// -------------------------------------------------------------------------
	
    private List<File> getQueryList() throws ConfigurationException {
    	
		String queryDir = props.getProperty(PROP_QUERY_DIR);
		String queryExt = props.getProperty(PROP_QUERY_EXT);
		if (queryDir == null)
			throw new ConfigurationException("missing query dir setting '" + PROP_QUERY_DIR + "' in " + cfgFile);
		if (queryExt == null)
			throw new ConfigurationException("missing query extension setting '" + PROP_QUERY_EXT + "' in " + cfgFile);

		File dir = new File(queryDir).getAbsoluteFile();
		if (!dir.isDirectory() || !dir.canRead())
			throw new IllegalArgumentException("not a readable query directory: " + dir);
		
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
	private String readQuery(File query) throws IOException {
		StringBuffer buffer = new StringBuffer();
		BufferedReader r = new BufferedReader(new FileReader(query));
		String input;
		while((input = r.readLine()) != null) {
			buffer.append(input).append("\n");
		}
		return buffer.toString();
	}
	
	/**
	 * Loads the repository configuration.
	 * 
	 * @param configFile the name of the repository configuration file.
	 * @param baseURI for resolving relative URIs in the configuration file.
	 * @return the repository configuration model.
	 * @throws ConfigurationException
	 */
//	private Graph loadRDFConfig(String configFile, String baseURI) throws ConfigurationException {
	private Graph loadRDFConfig(URI configFile, String baseURI) throws ConfigurationException {
		
		File file = new File(configFile).getAbsoluteFile();
//		RDFFormat format = Rio.getParserFormatForFileName(configFile);
		RDFFormat format = Rio.getParserFormatForFileName(configFile.getPath());
		if (format == null)
			throw new ConfigurationException("unknown RDF format of repository config: " + file);
		
		try {
//			Model model = new LinkedHashModel();
			Graph model = new GraphImpl();
			RDFParser parser = Rio.createParser(format);
			parser.setRDFHandler(new StatementCollector(model));
//			parser.parse(new FileReader(file), baseURI);
			parser.parse(configFile.toURL().openStream(), baseURI);
			return model;
			
		} catch (UnsupportedRDFormatException e) {
			throw new ConfigurationException("cannot load repository config, unsupported RDF format (" + format + "): " + file);
		} catch (RDFParseException e) {
			throw new ConfigurationException("cannot load repository config, RDF parser error: " + e.getMessage() + ": " + file);
		} catch (RDFHandlerException e) {
			throw new ConfigurationException("cannot load repository config, RDF handler error: " + e.getMessage() + ": " + file);
		} catch (IOException e) {
			throw new ConfigurationException("cannot load repository config, IO error: " + e.getMessage());
		}
	}

}
