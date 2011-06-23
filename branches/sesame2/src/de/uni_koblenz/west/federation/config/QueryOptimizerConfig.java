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
import static de.uni_koblenz.west.federation.config.FederationSailSchema.OPT_TYPE;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.SailConfigException;

/**
 * @author Olaf Goerlitz
 */
public class QueryOptimizerConfig extends AbstractSailConfig {
	
//	private static final String DEFAULT_OPTIMIZER = "DYNAMIC_PROGRAMMING";
//	private static final String DEFAULT_ESTIMATOR = CardinalityEstimatorType.STATISTICS.toString();
	
	private String estimatorType;
	
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

	@Override
	public Resource export(Graph model) {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		
		Resource self = super.export(model);
		
		model.add(self, ESTIMATOR, vf.createLiteral(this.estimatorType));
		
		return self;
	}

	@Override
	public void parse(Graph model, Resource implNode) throws SailConfigException {
		super.parse(model, implNode);
		
		Literal estimator = getObjectLiteral(model, implNode, ESTIMATOR);
		if (estimator != null) {
			this.estimatorType = estimator.getLabel();
		}
	}

	@Override
	public void validate() throws SailConfigException {
		super.validate();
		// TODO: check for valid optimizer setting
		
		if (this.estimatorType == null)
			throw new SailConfigException("no cardinality estimator specified: use " + ESTIMATOR);
		// TODO: check for valid estimator setting
	}

}