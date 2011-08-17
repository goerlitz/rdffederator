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
package de.uni_koblenz.west.statistics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.vocabulary.VOID2;

/**
 * Sesame repository keeping RDF statistics represented with Void 2 vocabulary.
 * 
 * @author Olaf Goerlitz
 */
public class Void2StatsRepository extends Void2Statistics {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Void2StatsRepository.class);
	private static final String USER_DIR = System.getProperty("user.dir") + File.separator;
	
	private static final ValueFactory uf = ValueFactoryImpl.getInstance();
	private static final URI DATASET = uf.createURI(VOID2.Dataset.toString());
	private static final URI ENDPOINT = uf.createURI(VOID2.sparqlEndpoint.toString());
	
	protected static final Void2StatsRepository singleton = new Void2StatsRepository();
	
	protected final Repository voidRepository;

	public static Void2StatsRepository getInstance() {
		return singleton;
	}
	
	private Void2StatsRepository() {
		this.voidRepository = new SailRepository(new MemoryStore());
		try {
			this.voidRepository.initialize();
		} catch (RepositoryException e) {
			throw new RuntimeException("initialization of statistics repository failed", e);
		}
	}
	
	@Override
	protected List<String> evalVar(String query, String var) {
		List<String> result = new ArrayList<String>();
		for (BindingSet bs : QueryExecutor.eval(voidRepository, query)) {
			result.add(bs.getValue(var).stringValue());
		}
		return result;
	}
	
	/**
	 * Extracts all SPARQL endpoints of the datasets.
	 * 
	 * @return the list of SPARQL endpoints.
	 */
	@Override
	public List<Graph> getEndpoints() {
		
		List<Graph> sources = new ArrayList<Graph>();
		
		ValueFactory uf = this.voidRepository.getValueFactory();
		URI voidDataset = uf.createURI(VOID2.Dataset.toString());
		URI sparqlEndpoint = uf.createURI(VOID2.sparqlEndpoint.toString());
		
		try {
			RepositoryConnection con = this.voidRepository.getConnection();
			
			try {
				for (Statement dataset : con.getStatements(null, RDF.TYPE, voidDataset, false).asList()) {
					for (Statement endpoint : con.getStatements(dataset.getSubject(), sparqlEndpoint, null, false).asList()) {
						sources.add(new Graph(endpoint.getObject().stringValue()));
					}
				}
			} catch (RepositoryException e) {
				e.printStackTrace();
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		
		return sources;
	}
	
	private List<URI> getEndpoints(URI voidURI, RepositoryConnection con) throws RepositoryException {
		ValueFactory uf = this.voidRepository.getValueFactory();
		URI voidDataset = uf.createURI(VOID2.Dataset.toString());
		URI sparqlEndpoint = uf.createURI(VOID2.sparqlEndpoint.toString());
		List<URI> endpoints = new ArrayList<URI>();
		
		for (Statement dataset : con.getStatements(null, RDF.TYPE, voidDataset, false, voidURI).asList()) {
			for (Statement endpoint : con.getStatements(dataset.getSubject(), sparqlEndpoint, null, false, voidURI).asList()) {
				// TODO: endpoint may be a literal
				endpoints.add((URI) endpoint.getObject());
			}
		}
		return endpoints;
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Loads the supplied voiD description into the statistics repository.
	 * 
	 * @param voidURI the URI of the voiD description to load.
	 * @return the assigned SPARQL endpoint.
	 */
	public URI load(URI voidURI, URI endpoint) throws IOException {
		if (voidURI == null)
			throw new IllegalArgumentException("voiD URI must not be null.");
		
		// initialize parser
		RDFFormat format = Rio.getParserFormatForFileName(voidURI.stringValue());
		if (format == null) {
			throw new IOException("Unsupported RDF format: " + voidURI);
		}

		URL voidURL = new URL(voidURI.stringValue());  // throws IOException
		InputStream in = voidURL.openStream();
		try {
			
			RepositoryConnection con = this.voidRepository.getConnection();
			try {
				
				// check if voiD description has already been loaded
				List<URI> endpoints = getEndpoints(voidURI, con);
				if (endpoints.size() > 0) {
					LOGGER.warn("VOID has already been loaded: " + voidURI);
					return endpoints.get(0);
				}
				
				// add voiD file content to repository
				con.add(in, voidURI.stringValue(), format, voidURI);
				
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("loaded VOID: " + voidURL.getPath().replace(USER_DIR, ""));
				
				if (endpoint == null) {
				
					// check if this voiD description has a valid SPARQL endpoint
					endpoints = getEndpoints(voidURI, con);

					if (endpoints.size() == 0)
						LOGGER.debug("found no SPARQL endpoint in voiD file");
					if (endpoints.size() > 1)
						// TODO: don't throw Exception but use first endpoint only
						throw new IllegalStateException("found multiple SPARQL endpoints in voiD file");

				return endpoints.iterator().next();
				} else {
					// find dataset resource in specified context
					RepositoryResult<Statement> result = con.getStatements(null, RDF.TYPE, DATASET, false, voidURI);
					Resource dataset = result.next().getSubject();
					
					// TODO: check that there is only one dataset defined
					
					// remove current SPARQL endpoint and add new one
					con.remove(dataset, ENDPOINT, null, voidURI);
					con.add(dataset, ENDPOINT, endpoint, voidURI);
					
					LOGGER.info("set SPARQL endpoint '" + endpoint + "' for " + voidURL.getPath().replace(USER_DIR, ""));
					
					return endpoint;
				}
				
			} catch (RepositoryException e) {
				e.printStackTrace();
			} catch (RDFParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			in.close();
		}
		
		return null;
	}
	
}
