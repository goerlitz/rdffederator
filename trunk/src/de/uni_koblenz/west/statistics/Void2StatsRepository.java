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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
//import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
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
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.QueryExecutor;
import de.uni_koblenz.west.vocabulary.VOID2;

/**
 * Sesame repository keeping RDF statistics represented with Void 2 vocabulary.
 * 
 * @author Olaf Goerlitz
 */
public class Void2StatsRepository extends Void2Statistics {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Void2StatsRepository.class);
	
	protected final Repository voidRepository;
	
	/**
	 * Creates a new Void 2 statistics representation based on a memory store.
	 */
	public Void2StatsRepository() {
		this.voidRepository = new SailRepository(new MemoryStore());
		try {
			this.voidRepository.initialize();
//		} catch (StoreException e) {
		} catch (RepositoryException e) {
			throw new RuntimeException("init of voiD 2 repository failed", e);
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
	
	// -------------------------------------------------------------------------
	
	/**
	 * Loads the supplied void description into the statistics repository.
	 * 
	 * @param the void description URL.
	 * @param the context of the loaded statistics.
	 */
	@Override
	public URI load(URL url) throws IOException {
		if (url == null)
			throw new IllegalArgumentException("voiD 2 description URL must not be null.");
		
		// initialize parser
		RDFFormat format = Rio.getParserFormatForFileName(url.getFile());
		if (format == null) {
			throw new IOException("Unsupported RDF format: " + url);
		}

		InputStream in = url.openStream();
		try {
			// TODO: check if this is valid void data with a sparql endpoint
			
			RepositoryConnection con = this.voidRepository.getConnection();
			try {
				URI context = url.toURI();
//				con.add(in, url.getFile(), format, voidRepository.getURIFactory().createURI(context.toString()));
				con.add(in, url.getFile(), format, voidRepository.getValueFactory().createURI(context.toString()));
				return context;
			} catch (URISyntaxException e) {
				e.printStackTrace();
//			} catch (StoreException e) {
			} catch (RepositoryException e) {
				e.printStackTrace();
			} catch (RDFParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				con.close();
			}
//		} catch (StoreException e) {
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		
		LOGGER.info("loaded voiD 2 data from " + url);
		return null;
	}
	
	public void setEndpoint(String endpoint, URI contextURI) {
		
//		URIFactory uf = this.voidRepository.getURIFactory();
		ValueFactory uf = this.voidRepository.getValueFactory();
		Value voidDataset = uf.createURI(VOID2.Dataset.toString());
		Resource context = uf.createURI(contextURI.toString());
		
		try {
			RepositoryConnection con = this.voidRepository.getConnection();
			
			// find dataset resource in specified context
//			ModelResult result = con.match(null, RDF.TYPE, voidDataset, false, context);
			RepositoryResult<Statement> result = con.getStatements(null, RDF.TYPE, voidDataset, false, context);
			Resource dataset = result.next().getSubject();
			
			// remove current sparql endpoint and add new one
//			con.removeMatch(dataset, uf.createURI(VOID2.sparqlEndpoint.toString()), null, context);
			con.remove(dataset, uf.createURI(VOID2.sparqlEndpoint.toString()), null, context);
			con.add(dataset, uf.createURI(VOID2.sparqlEndpoint.toString()), uf.createURI(endpoint), context);
			
			LOGGER.info("assigned new SPARQL endpoint '" + endpoint + "' for " + contextURI);
//		} catch (StoreException e) {
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
