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
package de.uni_koblenz.west.federation.config;

import static de.uni_koblenz.west.federation.config.FederationSailSchema.VOID_URI;
import static de.uni_koblenz.west.federation.config.VoidRepositorySchema.ENDPOINT;

import java.net.MalformedURLException;
import java.net.URL;

//import org.openrdf.model.Model;
//import org.openrdf.model.util.ModelException;
//import org.openrdf.store.StoreConfigException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;

/**
 * Configuration details for a void repository.
 * 
 * @author Olaf Goerlitz
 */
public class VoidRepositoryConfig extends RepositoryImplConfigBase {
	
	private URL voidUrl;
	private URL endpoint;
	
//	private String endpoint;
//	private URI sparqlEndpoint;
	
	public URL getVoidUrl() {
		return this.voidUrl;
	}
	
	public URL getEndpoint() {
		return this.endpoint;
	}
	
//	public URI getEndpoint() {
//		return this.sparqlEndpoint;
//	}

//	/**
//	 * Gets the SPARQL endpoint.
//	 * 
//	 * @return the SAPRQL endpoint.
//	 */
//	public String getEndpoint() {
//		return this.endpoint;
//	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Adds all Repository configuration settings to a configuration model.
	 * 
	 * @param model the configuration model to be filled.
	 * @return the resource representing this repository configuration.
	 */
	@Override
//	public Resource export(Model model) { // Sesame 3
	public Resource export(Graph model) { // Sesame 2
		Resource implNode = super.export(model);
		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();

		model.add(implNode, VOID_URI, vf.createURI(this.voidUrl.toString()));
		model.add(implNode, ENDPOINT, vf.createURI(this.endpoint.toString()));
//		if (voidUri != null) {
//			model.add(implNode, VOID_URI, voidUri);
//		}
//		if (endpoint != null) {
//			model.add(implNode, ENDPOINT, vf.createURI(endpoint));
//		}
		
		return implNode;
	}

	/**
	 * Parses the configuration model.
	 * 
	 * @param model the configuration model.
	 * @param implNode the resource representing this void repository.
	 */
	@Override
//	public void parse(Model model, Resource implNode) throws StoreConfigException { // Sesame 3
	public void parse(Graph model, Resource implNode) throws RepositoryConfigException { // Sesame 2
		super.parse(model, implNode);
		
//		try {
//			URI voidUri = model.filter(implNode, VOID_URI, null).objectURI();
			URI voidUri = (URI) model.match(implNode, VOID_URI, null).next().getObject();
			
			if (voidUri == null)
//				throw new StoreConfigException("VoidRepository requires: " + VOID_URI);
				throw new RepositoryConfigException("VoidRepository requires: " + VOID_URI);
			try {
				this.voidUrl = new URL(voidUri.stringValue());
			} catch (MalformedURLException e) {
//				throw new StoreConfigException("Malformed '" + VOID_URI + "' URL: " + voidUri);
				throw new RepositoryConfigException("Malformed '" + VOID_URI + "' URL: " + voidUri);
			}
			
//			URI endpoint = model.filter(implNode, ENDPOINT, null).objectURI();
			URI endpoint = (URI) model.match(implNode, ENDPOINT, null).next().getObject();
			
			if (endpoint == null)
//				throw new StoreConfigException("VoidRepository requires: " + ENDPOINT);
				throw new RepositoryConfigException("VoidRepository requires: " + ENDPOINT);
			try {
				this.endpoint = new URL(endpoint.stringValue());
			} catch (MalformedURLException e) {
//				throw new StoreConfigException("Malformed '" + ENDPOINT + "' URL: " + endpoint);
				throw new RepositoryConfigException("Malformed '" + ENDPOINT + "' URL: " + endpoint);
			}
			
//			this.sparqlEndpoint = model.filter(implNode, ENDPOINT, null).objectURI();
//			if (sparqlEndpoint != null) {
//				// TODO: do proper overwriting of sparql endpoint 
//				endpoint = sparqlEndpoint.toString();
//			}
//		}
//		catch (ModelException e) {
//			throw new StoreConfigException(e);
//		}
	}

//	/**
//	 * Validates this configuration. If the configuration is invalid a
//	 * {@link StoreConfigException} is thrown including the reason why the
//	 * configuration is invalid.
//	 * 
//	 * @throws StoreConfigException
//	 *             If the configuration is invalid.
//	 */
//	@Override
//	public void validate() throws StoreConfigException {
//		super.validate();
////		if (this.endpoint == null) {
////			throw new StoreConfigException("No SPARQL endpoint specified");
////		}
//	}

}
