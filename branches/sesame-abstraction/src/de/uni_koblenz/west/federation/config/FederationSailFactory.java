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

import java.util.ArrayList;
import java.util.List;

import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryRegistry;
import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;
//import org.openrdf.store.StoreConfigException;

import de.uni_koblenz.west.federation.FederationSail;

/**
 * A {@link SailFactory} that creates {@link FederationSail}s
 * based on the supplied configuration data.
 * 
 * ATTENTION: This factory must be published with full package name in
 *            META-INF/services/org.openrdf.sail.config.SailFactory
 * 
 * @author Olaf Goerlitz
 */
public class FederationSailFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "west:FederationSail";

	/**
	 * Returns the Sail's type: <tt>west:FederationSail</tt>.
	 * 
	 * @return the Sail's type.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	/**
	 * Provides a Sail configuration object for the configuration data.
	 * 
	 * @return a {@link FederationSailConfig}.
	 */
	public SailImplConfig getConfig() {
		return new FederationSailConfig();
	}

	/**
	 * Returns a Sail instance that has been initialized using the supplied
	 * configuration data.
	 * 
	 * @param config
	 *            the Sail configuration.
	 * @return The created (but un-initialized) Sail.
	 * @throws StoreConfigException
	 *             If no Sail could be created due to invalid or incomplete
	 *             configuration data.
	 */
	@Override
//	public Sail getSail(SailImplConfig config) throws StoreConfigException {
	public Sail getSail(SailImplConfig config) throws SailConfigException {

		if (!SAIL_TYPE.equals(config.getType())) {
//			throw new StoreConfigException("Invalid Sail type: " + config.getType());
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}
		assert config instanceof FederationSailConfig;
		FederationSailConfig fedConfig = (FederationSailConfig) config;

		// initialize federation members
		List<Repository> repositories = new ArrayList<Repository>();
		RepositoryFactory factory;
		for (RepositoryImplConfig member : fedConfig.getMemberConfigs()) {
			factory = RepositoryRegistry.getInstance().get(member.getType());
			if (factory == null) {
//				throw new StoreConfigException("Unsupported repository type: " + config.getType());
				throw new SailConfigException("Unsupported repository type: " + config.getType());
			}
//			repositories.add(factory.getRepository(member));
			try {
				repositories.add(factory.getRepository(member));
			} catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
		
		return new FederationSail(fedConfig.getProperties(), repositories);
	}

}
