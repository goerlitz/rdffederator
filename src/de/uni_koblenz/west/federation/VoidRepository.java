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
package de.uni_koblenz.west.federation;

import java.net.URL;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
//import org.openrdf.store.StoreException;

/**
 * Wrapper for remote repositories with voiD description and SPARQL endpoint.
 * 
 * @author Olaf Goerlitz
 */
public class VoidRepository extends HTTPRepository {
	
	protected final String endpoint;
	protected final URL voidUrl;

	public VoidRepository(URL voidUrl, URL endpoint) {
		super(endpoint.toString());
		this.endpoint = endpoint.toString();
		this.voidUrl = voidUrl;
	}
	
	public String getEndpoint() {
		return this.endpoint;
	}
	
	public URL getVoidUrl() {
		return this.voidUrl;
	}
	
	@Override
//	public RepositoryConnection getConnection() throws StoreException {
	public RepositoryConnection getConnection() throws RepositoryException {
		return new VoidRepositoryConnection(this);
	}	

}
