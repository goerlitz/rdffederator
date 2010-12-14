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

import static de.uni_koblenz.west.federation.config.FederationSailSchema.ESTIMATOR;
import static de.uni_koblenz.west.federation.config.FederationSailSchema.MEMBER;
import static de.uni_koblenz.west.federation.config.FederationSailSchema.OPTIMIZER;
//import static de.uni_koblenz.west.federation.config.FederationSailSchema.STATISTIC;
import static org.openrdf.repository.config.RepositoryImplConfigBase.create;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.openrdf.model.Graph;
//import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailImplConfigBase;
//import org.openrdf.store.StoreConfigException;

/**
 * Configuration details for federation setup including member descriptions.
 * 
 * @author Olaf Goerlitz
 */
public class FederationSailConfig extends SailImplConfigBase {
	
	private List<RepositoryImplConfig> members = new ArrayList<RepositoryImplConfig>();
	private Properties props = new Properties();
	
	private String optimizerType;
	private String estimatorType;
//	private String statisticsUrl;
	
	/**
	 * Gets the configuration settings of the federation members.
	 * 
	 * @return the member configuration settings.
	 */
	public List<RepositoryImplConfig> getMemberConfigs() {
		return members;
	}
	
	/**
	 * Gets the general federation settings.
	 * 
	 * @return the federation settings.
	 */
	public Properties getProperties() {
		if (props.size() == 0) {
			props.put("optimizer.type", optimizerType);
			props.put("estimator.type", estimatorType);
//			props.put("data.statistics", statisticsUrl);
		}
		return this.props;
	}
	
	// -------------------------------------------------------------------------

	/**
	 * Adds all Sail configuration settings to a configuration model.
	 * 
	 * @param model the configuration model to be filled.
	 * @return the resource representing this Sail configuration.
	 */
	@Override
//	public Resource export(Model model) {
	public Resource export(Graph model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		Resource self = super.export(model);
		
		for (RepositoryImplConfig member : this.members) {
			model.add(self, MEMBER, member.export(model));
		}
		model.add(self, OPTIMIZER, vf.createLiteral(optimizerType));
		model.add(self, ESTIMATOR, vf.createLiteral(estimatorType));
//		model.add(self, STATISTIC, vf.createLiteral(statisticsUrl));
		
		return self;
	}

	/**
	 * Parses the configuration model.
	 * 
	 * @param model the configuration model.
	 * @param implNode the resource representing this federation sail.
	 */
	@Override
//	public void parse(Model model, Resource implNode) throws StoreConfigException {
	public void parse(Graph model, Resource implNode) throws SailConfigException {
		super.parse(model, implNode);
		
		// get resources for all federation members
//		for (Value member : model.filter(implNode, MEMBER, null).objects()) {
//			create repository for the found member definition
//			this.members.add(create(model, (Resource) member));
//		}
		
		// Sesame 2:
		Iterator<Statement> objects = model.match(implNode, MEMBER, null);
		while (objects.hasNext()) {
			try {
				this.members.add(create(model, (Resource) objects.next().getObject()));
			} catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
		
		// Sesame 3:
//		optimizerType = model.filter(implNode, OPTIMIZER, null).objectString();
//		estimatorType = model.filter(implNode, ESTIMATOR, null).objectString();
//		statisticsUrl = model.filter(implNode, STATISTIC, null).objectString();
		
		// Sesame 2:
		optimizerType = model.match(implNode, OPTIMIZER, null).next().getObject().stringValue();
		estimatorType = model.match(implNode, ESTIMATOR, null).next().getObject().stringValue();
	}

	/**
	 * Validates this configuration. If the configuration is invalid a
	 * {@link StoreConfigException} is thrown including the reason why the
	 * configuration is invalid.
	 * 
	 * @throws StoreConfigException
	 *             If the configuration is invalid.
	 */
	@Override
//	public void validate() throws StoreConfigException {
	public void validate() throws SailConfigException {
		super.validate();
		if (members.size() == 0) {
//			throw new StoreConfigException("No federation members specified");
			throw new SailConfigException("No federation members specified");
		}
		for (RepositoryImplConfig cfg : members) {
//			cfg.validate();
			try {
				cfg.validate();
			} catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
	}

}
