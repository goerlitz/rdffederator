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

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * RDF Schema used by the federation configuration.
 * 
 * @author Olaf Goerlitz
 */
public class FederationSailSchema {
	
	private static final ValueFactory vf = ValueFactoryImpl.getInstance();
	
	/** The SailRepository schema namespace 
	 * (<tt>http://west.uni-koblenz.de/federation/config/sail#</tt>). */
	public static final String NAMESPACE = "http://west.uni-koblenz.de/config/federation/sail#";
	
	public static final URI MEMBER    = vf.createURI(NAMESPACE + "member");
	public static final URI OPTIMIZER = vf.createURI(NAMESPACE + "optimizer");
	public static final URI SRC_SLCTN = vf.createURI(NAMESPACE + "sourceSelection");
	public static final URI ESTIMATOR = vf.createURI(NAMESPACE + "estimator");
	public static final URI STATISTIC = vf.createURI(NAMESPACE + "statistic");
	public static final URI VOID_URI  = vf.createURI(NAMESPACE + "voidDescription");

}
