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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

//import org.openrdf.model.Model;
//import org.openrdf.model.impl.LinkedHashModel;
//import org.openrdf.store.StoreConfigException;
//import org.openrdf.store.StoreException;
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
import org.openrdf.sail.config.SailConfigException;

/**
 * Creates a repository from a config file.
 * 
 * @author Olaf Goerlitz
 */
public class RepositoryCreator {
	
	public Repository load(URL url) {
		try {
//			Model model = parse(url);
			Graph model = parse(url);
	    	RepositoryConfig repConf = RepositoryConfig.create(model, null);
	    	repConf.validate();
	    	RepositoryImplConfig implConf = repConf.getRepositoryImplConfig();
	    	RepositoryRegistry registry = RepositoryRegistry.getInstance();
	    	RepositoryFactory factory = registry.get(implConf.getType());
	    	if (factory == null) {
//	    		throw new StoreConfigException("Unsupported repository type: " + implConf.getType());
	    		throw new SailConfigException("Unsupported repository type: " + implConf.getType());
	    	}
	    	Repository repository = factory.getRepository(implConf);
	    	repository.initialize();
	    	return repository;
//		} catch (StoreConfigException e) {
		} catch (SailConfigException e) {
			e.printStackTrace();
		} catch (RepositoryConfigException e) {
			e.printStackTrace();
//		} catch (StoreException e) {
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

//	protected Model parse(URL url) throws StoreConfigException {
	protected Graph parse(URL url) throws SailConfigException {
		RDFFormat format = Rio.getParserFormatForFileName(url.getFile());
		if (format == null) {
//			throw new StoreConfigException("Unsupported file format: " + url.getFile());
			throw new SailConfigException("Unsupported file format: " + url.getFile());
		}

		try {
			RDFParser parser = Rio.createParser(format);

//			Model model = new LinkedHashModel();
			Graph model = new GraphImpl();
			parser.setRDFHandler(new StatementCollector(model));

			InputStream stream = url.openStream();
			try {
				parser.parse(stream, url.toString());
			}
			catch (RDFHandlerException e) {
				throw new AssertionError(e);
			}
			finally {
				stream.close();
			}

			return model;
		}
		catch (UnsupportedRDFormatException e) {
//			throw new StoreConfigException("Failed to find suitable file parser " + url.getFile());
			throw new SailConfigException("Failed to find suitable file parser " + url.getFile());
		}
		catch (RDFParseException e) {
//			throw new StoreConfigException("Failed to parse configuration file " + url.getFile() + ": "
			throw new SailConfigException("Failed to parse configuration file " + url.getFile() + ": "
					+ e.getMessage());
		}
		catch (IOException e) {
//			throw new StoreConfigException("Failed to read configuration file " + url.getFile() + ": "
			throw new SailConfigException("Failed to read configuration file " + url.getFile() + ": "
					+ e.getMessage(), e);
		}
	}
	
}
