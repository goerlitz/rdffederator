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

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.sail.config.SailConfigException;

import de.uni_koblenz.west.federation.sources.SourceSelector;

/**
 * @author Olaf Goerlitz
 */
public class SourceSelectorFactory {
	
//	private SourceSelectorFactory() {
//	}
//	
//	public static SourceSelectorFactory newInstance() {
//		return new SourceSelectorFactory();
//	}
	
	public static SourceSelectorConfig createConfig(Graph model, Resource implNode) throws SailConfigException {
		SourceSelectorConfig config = new SourceSelectorConfig();
		config.parse(model, implNode);
		config.validate();
		return config;
	}
	
	public static SourceSelector getSourceSelector(SourceSelectorConfig config) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	// -------------------------------------------------------------------------
	
//	protected static String getObjectString(Graph model, Resource implNode, URI property) throws SailConfigException {
//		Iterator<Statement> objects = model.match(implNode, property, null);
//		if (!objects.hasNext())
//			throw new SailConfigException("found no value for " + implNode);
//		Statement st = objects.next();
//		if (objects.hasNext())
//			throw new SailConfigException("found multiple values for " + implNode);
//		return st.getObject().stringValue();
//	}
		

}
