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
package de.uni_koblenz.west.federation.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

/**
 * Configuration object for test scenarios.
 * 
 * @author Olaf Goerlitz
 */
public class Configuration {
	
	private static final String PROP_REP_CONFIG = "repository.config";
	
	private File cfgFile;
	private Properties props = new Properties();
	
	private Configuration(String fileName) throws IOException {
		this.cfgFile = new File(fileName).getAbsoluteFile();
		this.props.load(new FileReader(this.cfgFile));
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
			RepositoryConfig repConf = RepositoryConfig.create(loadRDFConfig(repConfig), null);
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
	
	/**
	 * Loads the repository configuration.
	 * 
	 * @param configFile the name of the repository configuration file.
	 * @return the repository configuration model.
	 * @throws ConfigurationException
	 */
	private Graph loadRDFConfig(String configFile) throws ConfigurationException {
		
		File file = new File(configFile).getAbsoluteFile();
		RDFFormat format = Rio.getParserFormatForFileName(configFile);
		if (format == null)
			throw new ConfigurationException("unknown RDF format of repository config: " + file);
		
		try {
//			Model model = new LinkedHashModel();
			Graph model = new GraphImpl();
			RDFParser parser = Rio.createParser(format);
			parser.setRDFHandler(new StatementCollector(model));

			// need file URI to resolve relative URIs in config file
			parser.parse(new FileReader(file), file.toURI().toString());
			return model;
			
		} catch (UnsupportedRDFormatException e) {
			throw new ConfigurationException("cannot read repository config, unsupported RDF format (" + format + "): " + file);
		} catch (RDFParseException e) {
			throw new ConfigurationException("cannot read repository config, RDF parser error: " + e.getMessage() + ": " + file);
		} catch (RDFHandlerException e) {
			throw new ConfigurationException("cannot read repository config, RDF handler error: " + e.getMessage() + ": " + file);
		} catch (IOException e) {
			throw new ConfigurationException(e.getMessage());
		}
	}

}
