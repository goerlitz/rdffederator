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
package de.uni_koblenz.west.splendid.config;

import static de.uni_koblenz.west.splendid.config.FederationSailSchema.VOID_URI;
import static de.uni_koblenz.west.splendid.config.VoidRepositorySchema.ENDPOINT;

import java.util.Iterator;

//import org.openrdf.model.Model;
//import org.openrdf.model.util.ModelException;
//import org.openrdf.store.StoreConfigException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;
import org.openrdf.sail.config.SailConfigException;

/**
 * Configuration details for a void repository.
 * 
 * @author Olaf Goerlitz
 */
public class VoidRepositoryConfig extends RepositoryImplConfigBase {
	
	private URI voidUri;
	private URI endpoint;
	
	/**
	 * Returns the location of the VOID file.
	 * 
	 * @return the location of the VOID file or null if it is not set.
	 */
	public URI getVoidURI() {
		return this.voidUri;
	}
	
	/**
	 * Returns the location of the SPARQL endpoint.
	 * 
	 * @return the location of the SPARQL endpoint or null if it is not set.
	 */
	public URI getEndpoint() {
		return this.endpoint;
	}

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

		model.add(implNode, VOID_URI, this.voidUri);
		
		if (this.endpoint != null)
			model.add(implNode, ENDPOINT, this.endpoint);
		
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
		
		this.voidUri = getObjectURI(model, implNode, VOID_URI);
		if (this.voidUri == null)
//			throw new StoreConfigException("VoidRepository requires: " + VOID_URI);  // Sesame 3
			throw new RepositoryConfigException("VoidRepository requires: " + VOID_URI);
		
		this.endpoint = getObjectURI(model, implNode, ENDPOINT);
		
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
	
	/**
	 * Returns the object URI of the setting with the specified property.
	 * 
	 * @param config the configuration settings.
	 * @param subject the subject (sub context) of the configuration setting.
	 * @param property the configuration property.
	 * @return the URI value of the desired property setting or null.
	 * @throws SailConfigException if there is no (single) URI to return.
	 */
	protected URI getObjectURI(Graph config, Resource subject, URI property) throws RepositoryConfigException {
		Iterator<Statement> objects = config.match(subject, property, null);
		if (!objects.hasNext())
			return null;
//			throw new RepositoryConfigException("found no settings for property " + property);
		Statement st = objects.next();
		if (objects.hasNext())
			throw new RepositoryConfigException("found multiple settings for property " + property);
		Value object = st.getObject();
		if (object instanceof URI)
			return (URI) object;
		else
			throw new RepositoryConfigException("property value is not a URI: " + property + " " + object); 
	}

}
