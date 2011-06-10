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
import static de.uni_koblenz.west.federation.config.FederationSailSchema.SRC_SLCTN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryImplConfigBase;
import org.openrdf.sail.config.SailConfigException;

/**
 * Configuration details for federation setup including member descriptions.
 * 
 * @author Olaf Goerlitz
 */
public class FederationSailConfig extends AbstractSailConfig {
	
	private final List<RepositoryImplConfig> memberConfig = new ArrayList<RepositoryImplConfig>();
	private SourceSelectorConfig selectorConfig;
	
	private String optimizerType;
	private String estimatorType;
//	private String statisticsUrl;
	
	/**
	 * Returns the configuration settings of the federation members.
	 * 
	 * @return the member repository configuration settings.
	 */
	public List<RepositoryImplConfig> getMemberConfigs() {
		return this.memberConfig;
	}
	
	/**
	 * Returns the configuration settings of the source selector.
	 * 
	 * @return the source selection configuration settings.
	 */
	public SourceSelectorConfig getSelectorConfig() {
		return this.selectorConfig;
	}
	
	public String getOptimizerType() {
		return this.optimizerType;
	}
	
	public String getEstimatorType() {
		return this.estimatorType;
	}
	
	// -------------------------------------------------------------------------

	/**
	 * Adds all Sail configuration settings to a configuration model.
	 * 
	 * @param model the configuration model to be filled.
	 * @return the resource representing this Sail configuration.
	 */
	@Override
//	public Resource export(Model model) { // Sesame 3
	public Resource export(Graph model) { // Sesame 2
		ValueFactory vf = ValueFactoryImpl.getInstance();
		
		Resource self = super.export(model);
		
		for (RepositoryImplConfig member : this.memberConfig) {
			model.add(self, MEMBER, member.export(model));
		}
		model.add(self, SRC_SLCTN, this.selectorConfig.export(model));
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
//	public void parse(Model model, Resource implNode) throws StoreConfigException { // Sesame 3
	public void parse(Graph model, Resource implNode) throws SailConfigException { // Sesame 2
		super.parse(model, implNode);
		
		// extract the repository settings for all defined federation members
//		for (Value member : model.filter(implNode, MEMBER, null).objects()) { // Sesame 3
		for (Value member : filter(model, implNode, MEMBER)) { // Sesame 2
			if (member instanceof Resource) {
				try {
					this.memberConfig.add(RepositoryImplConfigBase.create(model, (Resource) member));
				} catch (RepositoryConfigException e) {
					throw new SailConfigException(e);
				}
			}
			else {
				throw new SailConfigException("Found literal for federation member node, expected a resource");
			}
		}
		
		// get source selection strategy
		selectorConfig = SourceSelectorFactory.createConfig(model, getObjectResource(model, implNode, SRC_SLCTN));
		
		// extract the query processing strategy
		optimizerType = getOption(model, implNode, OPTIMIZER);
		
		estimatorType = getOption(model, implNode, ESTIMATOR);
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
//	public void validate() throws StoreConfigException { // Sesame 3
	public void validate() throws SailConfigException { // Sesame 2
		super.validate();
		if (memberConfig.size() == 0) {
//			throw new StoreConfigException("No federation members specified"); // Sesame 3
			throw new SailConfigException("No federation members specified"); // Sesame 2
		}
		
		// validate all member repositories
		for (RepositoryImplConfig cfg : memberConfig) {
			try {
				cfg.validate();
			} catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
		
		this.selectorConfig.validate();
		
//		if (sourceSelector == null)
//			throw new SailConfigException("no source selection strategy specified: use " + SRC_SLCTN);
		
		if (optimizerType == null)
			throw new SailConfigException("no query optimization strategy specified: use " + OPTIMIZER);
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Helper method to extract a string-valued configuration option.
	 * 
	 * @param model the configuration model
	 * @param implNode node representing a specific configuration context.
	 * @param option configuration option to look for.
	 * @return the string value of the configuration option or null.
	 */
//	private String getOption(Model model, Resource implNode, URI option) throws StoreConfigException { // Sesame 3
	private String getOption(Graph model, Resource implNode, URI option) throws SailConfigException { // Sesame 2
//		return model.filter(implNode, option, null).objectString(); // Sesame 3
		return model.match(implNode, option, null).next().getObject().stringValue(); // Sesame 2
	}
	
	/**
	 * Helper method to extract a configuration's sub setting.
	 * 
	 * @param model the configuration model
	 * @param implNode node representing a specific configuration context.
	 * @param option configuration option to look for
	 * @return set of found values for the configuration setting.
	 */
//	private Set<Value> filter(Model model, Resource implNode, URI option) { // Sesame 3
	private Set<Value> filter(Graph model, Resource implNode, URI option) { // Sesame 2
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
