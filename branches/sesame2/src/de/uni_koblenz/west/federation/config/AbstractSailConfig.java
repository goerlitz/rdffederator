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

import java.util.Iterator;

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
			throw new SailConfigException("No type specified for implementation");
		}
	}
	
	// -------------------------------------------------------------------------
	
	protected Literal getObjectLiteral(Graph model, Resource implNode, URI property) throws SailConfigException {
		Iterator<Statement> objects = model.match(implNode, property, null);
		if (!objects.hasNext())
			throw new SailConfigException("found no object value for " + property);
		Statement st = objects.next();
		if (objects.hasNext())
			throw new SailConfigException("found multiple object values for " + property);
		Value object = st.getObject();
		if (object instanceof Literal)
			return (Literal) object;
		else
			throw new SailConfigException("no a literal object: " + property + " " + object); 
	}
	
	protected URI getObjectURI(Graph model, Resource implNode, URI property) throws SailConfigException {
		Iterator<Statement> objects = model.match(implNode, property, null);
		if (!objects.hasNext())
			throw new SailConfigException("found no object value for " + property);
		Statement st = objects.next();
		if (objects.hasNext())
			throw new SailConfigException("found multiple object values for " + property);
		Value object = st.getObject();
		if (object instanceof URI)
			return (URI) object;
		else
			throw new SailConfigException("not a URI object: " + property + " " + object); 
	}
}
