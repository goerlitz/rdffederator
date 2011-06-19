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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryRegistry;
import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;

import de.uni_koblenz.west.federation.FederationOptimizer;
import de.uni_koblenz.west.federation.FederationOptimizerFactory;
import de.uni_koblenz.west.federation.FederationSail;
import de.uni_koblenz.west.federation.VoidRepository;
import de.uni_koblenz.west.federation.helpers.Format;
import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.federation.sources.IndexSelector;
import de.uni_koblenz.west.federation.sources.SourceSelector;
import de.uni_koblenz.west.federation.sources.SparqlAskSelector;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType;
import de.uni_koblenz.west.optimizer.eval.CostCalculator;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.eval.QueryModelEvaluator;
import de.uni_koblenz.west.optimizer.rdf.BGPOperator;
import de.uni_koblenz.west.optimizer.rdf.eval.QueryModelVerifier;
import de.uni_koblenz.west.statistics.Void2StatsRepository;

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
	 * @throws SailConfigException
	 *             If no Sail could be created due to invalid or incomplete
	 *             configuration data.
	 */
	@Override
//	public Sail getSail(SailImplConfig config) throws StoreConfigException { // Sesame 3
	public Sail getSail(SailImplConfig config) throws SailConfigException { // Sesame 2

		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}
		assert config instanceof FederationSailConfig;
		FederationSailConfig cfg = (FederationSailConfig)config;
		FederationSail sail = new FederationSail();
		
		// Create all member repositories
		List<Repository> members = new ArrayList<Repository>();
		RepositoryRegistry registry = RepositoryRegistry.getInstance();
		
		for (RepositoryImplConfig repConfig : cfg.getMemberConfigs()) {
			RepositoryFactory factory = registry.get(repConfig.getType());
			if (factory == null) {
				throw new SailConfigException("Unsupported repository type: " + repConfig.getType());
			}
			try {
				members.add(factory.getRepository(repConfig));
			} catch (RepositoryConfigException e) {
				throw new SailConfigException("invalid repository configuration: " + e.getMessage(), e);
			}
		}
		sail.setMembers(members);

		// Create void statistics for member repositories
		List<Graph> sources = new ArrayList<Graph>();
		Void2StatsRepository stats = new Void2StatsRepository();
		
		for (Repository rep : members) {
			if (rep instanceof VoidRepository) {
				VoidRepository voidRep = (VoidRepository) rep;
				URI context;
				try {
					context = stats.load(voidRep.getVoidUrl());
				} catch (IOException e) {
					throw new SailConfigException("can not read voiD description: " + voidRep.getVoidUrl() + e.getMessage(), e);
				}
				stats.setEndpoint(voidRep.getEndpoint(), context);
				sources.add(new Graph(voidRep.getEndpoint()));
			}
		}
		sail.setStatistics(stats);
		
		// Create source selector from configuration settings
		SourceSelector selector;
		SourceSelectorConfig selConf = cfg.getSelectorConfig();
		String selectorType = selConf.getType();
		
		if ("ASK".equalsIgnoreCase(selectorType))
			selector = new SparqlAskSelector(sources, selConf.isGroupBySameAs());
		else if ("STATS".equalsIgnoreCase(selectorType))
			selector = new IndexSelector(stats, selConf.isUseTypeStats());
		else {
			throw new SailConfigException("no source selector specified");
		}
		sail.setSourceSelector(selector);
		
		// Create optimizer from configuration settings
		FederationOptimizer optimizer;
		QueryOptimizerConfig optConf = cfg.getOptimizerConfig();
		String optimizerType = optConf.getType();
		String estimatorType = optConf.getEstimatorType();
		
		CostModel costModel = new CostModel();
		FederationOptimizerFactory factory = new FederationOptimizerFactory();
		factory.setStatistics(stats);
		factory.setSourceSelector(selector);
		factory.setCostmodel(costModel);
		optimizer = factory.getOptimizer(optimizerType, estimatorType);
		
		// enable optimization result verification
		optimizer.setResultVerifier(createOptimizationVeryfier(factory.getCostCalculator(CardinalityEstimatorType.valueOf(estimatorType), costModel)));
		
		sail.setFederationOptimizer(optimizer);
		
		return sail;
	}
	
	private QueryModelVerifier<StatementPattern, ValueExpr> createOptimizationVeryfier(final CostCalculator<BGPOperator<StatementPattern, ValueExpr>> costEval) {
		return new QueryModelVerifier<StatementPattern, ValueExpr>() {
			@Override
			public QueryModelEvaluator<BGPOperator<StatementPattern, ValueExpr>, ? extends Number> getEvaluator() {
				return costEval;
			}
			@Override
			public void resultObtained(Number value) {
				System.out.println(Format.d(value.doubleValue(), 2));
			}
		};
	}

}
