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
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.FederationSail;

/**
 * Utility class that creates a repository from configuration settings.
 * 
 * @author Olaf Goerlitz
 */
public class ConfigurationManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);

	/**
	 * Creates a repository based on the supplied configuration file.
	 * 
	 * @param repConfig the name of the repository configuration file to use.
	 * @return the initialized repository.
	 * @throws RepositoryConfigException if reading the configuration failed.
	 * @throws RepositoryException if the initializations failed.
	 */
	public static Repository createRepository(String repConfig) throws RepositoryConfigException, RepositoryException {
    	RepositoryConfig repConf = RepositoryConfig.create(loadRDFConfig(repConfig), null);
    	repConf.validate();
    	RepositoryImplConfig implConf = repConf.getRepositoryImplConfig();
    	RepositoryRegistry registry = RepositoryRegistry.getInstance();
    	RepositoryFactory factory = registry.get(implConf.getType());
    	if (factory == null) {
//    		throw new StoreConfigException("Unsupported repository type: " + implConf.getType());
    		throw new RepositoryConfigException("Unsupported repository type: " + implConf.getType());
    	}
    	Repository repository = factory.getRepository(implConf);
    	repository.initialize();
    	return repository;
	}
	
	/**
	 * Loads the repository configuration.
	 * 
	 * @param configFile the name of the repository configuration file.
	 * @return the repository configuration model.
	 * @throws RepositoryConfigException
	 */
	private static Graph loadRDFConfig(String configFile) throws RepositoryConfigException {
		File file = new File(configFile).getAbsoluteFile();
		
		RDFFormat format = Rio.getParserFormatForFileName(configFile);
		if (format == null)
			throw new RepositoryConfigException("unknown RDF format of repository config: " + file);
		
		try {
			RDFParser parser = Rio.createParser(format);
//			Model model = new LinkedHashModel();
			Graph model = new GraphImpl();
			parser.setRDFHandler(new StatementCollector(model));

			// need file URI to resolve relative URIs in config file
			parser.parse(new FileReader(file), file.toURI().toString());
			return model;
		} catch (UnsupportedRDFormatException e) {
			throw new RepositoryConfigException("unsupported RDF format (" + format + ") of repository config: " + file);
		} catch (RDFParseException e) {
			throw new RepositoryConfigException("parser error: " + e.getMessage() + " in " + file);
		} catch (RDFHandlerException e) {
			throw new RepositoryConfigException("handler error: " + e.getMessage() + " in " + file);
		} catch (IOException e) {
			throw new RepositoryConfigException(e.getMessage());
		}
	}
	
}
