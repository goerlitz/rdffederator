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
package de.uni_koblenz.west.federation.config;

import static org.openrdf.sail.config.SailConfigSchema.SAILTYPE;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailImplConfig;

/**
 * Generic configuration object for managing sail configuration settings
 * of a certain type. In contrast to SailImplConfigBase, which only supports
 * sail:sailType, it can be used for configuration options of different types. 
 * 
 * @author Olaf Goerlitz
 */
public abstract class AbstractSailConfig implements SailImplConfig {
	
	private String type;
	private URI typePredicate;
	
	protected AbstractSailConfig() {
		this.typePredicate = SAILTYPE;
	}
	
	protected AbstractSailConfig(URI typePredicate) {
		this.typePredicate = typePredicate;
	}
	
	@Override
	public String getType() {
		return type;
	}

	@Override
	public Resource export(Graph model) {
		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();

		BNode implNode = vf.createBNode();

		if (type != null) {
			model.add(implNode, this.typePredicate, vf.createLiteral(type));
		}

		return implNode;
	}

	@Override
	public void parse(Graph model, Resource implNode) throws SailConfigException {
		Literal typeLit = getObjectLiteral(model, implNode, this.typePredicate);
		if (typeLit != null) {
			this.type = typeLit.getLabel();
		}
	}

	@Override
	public void validate() throws SailConfigException {
		if (type == null) {
			throw new SailConfigException("No implementation type specified: use " + this.typePredicate);
		}
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the object resource of the triple matching the supplied predicate.
	 * 
	 * @param model the model of the configuration settings.
	 * @param implNode the model representing a configuration setting.
	 * @param predicate the predicate defining a configuration attribute.
	 * @return the resource representing the configuration attribute or null.
	 * @throws SailConfigException if there is no (single) resource to return.
	 */
	protected Literal getObjectLiteral(Graph model, Resource implNode, URI property) throws SailConfigException {
		Iterator<Statement> objects = model.match(implNode, property, null);
		if (!objects.hasNext())
			return null;
		Statement st = objects.next();
		if (objects.hasNext())
			throw new SailConfigException("found multiple object values for " + property);
		Value object = st.getObject();
		if (object instanceof Literal)
			return (Literal) object;
		else
			throw new SailConfigException("object value is not a Literal: " + property + " " + object); 
	}
	
	/**
	 * Returns the object resource of the triple matching the supplied predicate.
	 * 
	 * @param model the model of the configuration settings.
	 * @param implNode the model representing a configuration setting.
	 * @param predicate the predicate defining a configuration attribute.
	 * @return the resource representing the configuration attribute or null.
	 * @throws SailConfigException if there is no (single) resource to return.
	 */
	protected Resource getObjectResource(Graph model, Resource implNode, URI predicate) throws SailConfigException {
		Iterator<Statement> objects = model.match(implNode, predicate, null);
		if (!objects.hasNext())
			throw new SailConfigException("found no object value for " + predicate);
		Statement st = objects.next();
		if (objects.hasNext())
			throw new SailConfigException("found multiple object values for " + predicate);
		Value object = st.getObject();
		if (object instanceof Resource)
			return (Resource) object;
		else
			throw new SailConfigException("object value is not a Resource: " + predicate + " " + object); 
	}
	
	/**
	 * Helper method to extract a configuration's sub setting.
	 * 
	 * @param model the configuration model
	 * @param implNode node representing a specific configuration context.
	 * @param option configuration option to look for
	 * @return set of found values for the configuration setting.
	 */
//	protected Set<Value> filter(Model model, Resource implNode, URI option) { // Sesame 3
	protected Set<Value> filter(Graph model, Resource implNode, URI option) { // Sesame 2
//		return model.filter(implNode, MEMBER, null).objects(); // Sesame 3
		// Sesame 2:
		Set<Value> values = new HashSet<Value>();
		Iterator<Statement> objects = model.match(implNode, option, null);
		while (objects.hasNext()) {
			values.add(objects.next().getObject());
		}
		return values;
	}

}
