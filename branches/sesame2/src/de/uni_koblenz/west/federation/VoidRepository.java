/*
 * This file is part of RDF Federator.
 * Copyright 2011 Olaf Goerlitz
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

import java.io.File;
import java.io.IOException;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import de.uni_koblenz.west.federation.config.VoidRepositoryConfig;
import de.uni_koblenz.west.statistics.Void2StatsRepository;

/**
 * A proxy for a remote repository which is accessed via a SPARQL endpoint.
 * 
 * @author Olaf Goerlitz
 */
public class VoidRepository implements Repository {
	
	protected final ValueFactory vf = new ValueFactoryImpl();
	protected URI endpoint;
	protected final URI voidUrl;
	
	public VoidRepository(VoidRepositoryConfig config) {
		this.endpoint = config.getEndpoint();
		this.voidUrl = config.getVoidURI();
	}
	
	public URI getEndpoint() {
		return this.endpoint;
	}

//public URL getVoidUrl() {
//	return this.voidUrl;
//}
	
	// --------------------------------------------------------------
	
	@Override
	public void setDataDir(File dataDir) {
		throw new UnsupportedOperationException("SPARQL endpoint repository has no data dir");
	}

	@Override
	public File getDataDir() {
		throw new UnsupportedOperationException("SPARQL endpoint repository has no data dir");
	}
	
	@Override
	public boolean isWritable() throws RepositoryException {
		return false;
	}

	@Override
	public ValueFactory getValueFactory() {
		return this.vf;
	}

	@Override
	public void initialize() throws RepositoryException {
		
		try {
			URI voidEndpoint = Void2StatsRepository.getInstance().load(this.voidUrl);
			if (this.endpoint == null) {
				this.endpoint = voidEndpoint;
			} else {
				Void2StatsRepository.getInstance().setEndpoint(this.endpoint, this.voidUrl);
			}
		} catch (IOException e) {
			throw new RepositoryException("can not read voiD description: " + this.voidUrl + e.getMessage(), e);
		}		
	}

	@Override
	public void shutDown() throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return new VoidRepositoryConnection(this);
	}	

}
