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
import static de.uni_koblenz.west.federation.config.FederationSailSchema.GROUP_BY_SAMEAS;
import static de.uni_koblenz.west.federation.config.FederationSailSchema.GROUP_BY_SOURCE;
import static de.uni_koblenz.west.federation.config.FederationSailSchema.USE_BIND_JOIN;
import static de.uni_koblenz.west.federation.config.FederationSailSchema.USE_HASH_JOIN;
import static de.uni_koblenz.west.federation.config.FederationSailSchema.OPT_TYPE;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.SailConfigException;

/**
 * Configuration settings for the query optimizer.
 * 
 * @author Olaf Goerlitz
 */
public class QueryOptimizerConfig extends AbstractSailConfig {
	
	private String estimatorType;
	
	private boolean groupBySameAs;
	private boolean groupBySource;
	
	private boolean useBindJoin;
	private boolean useHashJoin;
	
	protected QueryOptimizerConfig() {
		super(OPT_TYPE);
	}
	
	public static QueryOptimizerConfig create(Graph model, Resource implNode) throws SailConfigException {
		QueryOptimizerConfig config = new QueryOptimizerConfig();
		config.parse(model, implNode);
		return config;
	}
	
	public String getEstimatorType() {
		return this.estimatorType;
	}
	
	public boolean isGroupBySameAs() {
		return this.groupBySameAs;
	}

	public boolean isGroupBySource() {
		return this.groupBySource;
	}

	
	public boolean isUseBindJoin() {
		return this.useBindJoin;
	}
	
	public boolean isUseHashJoin() {
		return this.useHashJoin;
	}

	@Override
	public Resource export(Graph model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		
		Resource self = super.export(model);
		
		model.add(self, ESTIMATOR, vf.createLiteral(this.estimatorType));
		
		model.add(self, GROUP_BY_SAMEAS, vf.createLiteral(this.groupBySameAs));
		model.add(self, GROUP_BY_SOURCE, vf.createLiteral(this.groupBySource));
		
		model.add(self, USE_BIND_JOIN, vf.createLiteral(this.useBindJoin));
		model.add(self, USE_HASH_JOIN, vf.createLiteral(this.useHashJoin));
		
		return self;
	}

	@Override
	public void parse(Graph model, Resource implNode) throws SailConfigException {
		super.parse(model, implNode);
		
		Literal estimator = getObjectLiteral(model, implNode, ESTIMATOR);
		if (estimator != null) {
			this.estimatorType = estimator.getLabel();
		}
		
		groupBySameAs = getObjectBoolean(model, implNode, GROUP_BY_SAMEAS, false);
		groupBySource = getObjectBoolean(model, implNode, GROUP_BY_SOURCE, false);
		
		useBindJoin = getObjectBoolean(model, implNode, USE_BIND_JOIN, false);
		useHashJoin = getObjectBoolean(model, implNode, USE_HASH_JOIN, false);
	}

	@Override
	public void validate() throws SailConfigException {
		super.validate();
		// TODO: check for valid optimizer settings
		
		if (this.estimatorType == null)
			throw new SailConfigException("no cardinality estimator specified: use " + ESTIMATOR);
		// TODO: check for valid estimator settings
	}

}
