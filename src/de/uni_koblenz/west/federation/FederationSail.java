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
package de.uni_koblenz.west.federation;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

//import org.openrdf.model.LiteralFactory;
//import org.openrdf.model.URIFactory;
//import org.openrdf.model.impl.LiteralFactoryImpl;
//import org.openrdf.model.impl.URIFactoryImpl;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
//import org.openrdf.sail.federation.Federation;
import org.openrdf.sail.helpers.SailBase;
//import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.adapter.SesameAdapter;
import de.uni_koblenz.west.optimizer.eval.CardinalityEstimatorType;
import de.uni_koblenz.west.optimizer.eval.CostModel;
import de.uni_koblenz.west.optimizer.rdf.SourceFinder;
import de.uni_koblenz.west.statistics.Void2StatsRepository;

/**
 * Wraps multiple data sources (remote repositories) within a single
 * Sail interface. Queries fragments are sent to a selected subsets
 * of the data sources. Join optimization is based on data statistics
 * and result cardinality estimation.<br>
 * 
 * The implementation is adapted from Sesame's {@link Federation} Sail.
 * 
 * @author Olaf Goerlitz
 * @see org.openrdf.sail.federation.Federation
 */
public class FederationSail extends SailBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FederationSail.class);
	
	private static final String DEFAULT_OPTIMIZER = "DYNAMIC_PROGRAMMING";
	private static final String DEFAULT_ESTIMATOR = CardinalityEstimatorType.STATISTICS.toString();
	
	private Void2StatsRepository stats = new Void2StatsRepository();
	private List<Repository> members = new ArrayList<Repository>();
	private SourceFinder<StatementPattern> finder;
	private QueryOptimizer optimizer;
	
	String optStrategy;
	String estimatorType;
	private boolean initialized = false;
	
	/**
	 * Creates a federation Sail using the properties and federation members.
	 * 
	 * @param config the Sail's configuration settings.
	 * @param members the federation members.
	 */
	public FederationSail(Properties config, List<Repository> members) {
		
		if (config == null)
			throw new IllegalArgumentException("config is NULL");
		if (members == null)
			throw new IllegalArgumentException("member list is NULL");
		
		// set federation members
		this.members = members;
		
		// set optimizer type
		optStrategy = config.getProperty("optimizer.type");
		if (optStrategy == null) {
			optStrategy = DEFAULT_OPTIMIZER;
			LOGGER.debug("using default optimization strategy: " + DEFAULT_OPTIMIZER);
		}
		
		// set cardinality estimator type (statistics-based or true counts)
		estimatorType = config.getProperty("estimator.type");
		if (estimatorType == null) {
			estimatorType = DEFAULT_ESTIMATOR;
			LOGGER.debug("using default estimator: " + DEFAULT_ESTIMATOR);
		}
	}
	
	public QueryOptimizer getFederationOptimizer() {
		return this.optimizer;
	}
	
	public SourceFinder<StatementPattern> getSourceFinder() {
		return this.finder;
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Initializes the Sail with statistics and optimizer settings.
	 * 
	 * @throws StoreException
	 *         If the Sail could not be initialized.
	 */
	@Override
//	public void initialize() throws StoreException {
	public void initialize() throws SailException {
		
		// only Sesame 2 needs to initialize super class
		super.initialize();
		
		// initialize member repositories
		for (Repository rep : this.members) {
			try {
				if (rep instanceof VoidRepository) {
					VoidRepository voidRep = (VoidRepository) rep;
					URI context = stats.load(voidRep.getVoidUrl());
					stats.setEndpoint(voidRep.getEndpoint(), context);
				}
				rep.initialize();
			} catch (IOException e) {
				LOGGER.error("can not read voiD description: " + e.getMessage());
//				throw new StoreException("can not read voiD description: " + e.getMessage(), e);
				throw new SailException("can not read voiD description: " + e.getMessage(), e);
			} catch (RepositoryException e) {
				throw new SailException("can not initialize void repository: " + e.getMessage(), e);
			} catch (IllegalStateException e) {
				LOGGER.debug("member repository is already initialized", e);
			}
		}

		this.finder = new SourceFinder<StatementPattern>(stats, new SesameAdapter());
		
		// initialize optimizer
		FederationOptimizerFactory factory = new FederationOptimizerFactory();
		factory.setStatistics(stats);
		factory.setSourceFinder(finder);
		factory.setCostmodel(new CostModel());
		this.optimizer = factory.getOptimizer(optStrategy, estimatorType);
		
		initialized = true;
	}
	
	/**
	 * Shuts down the Sail.
	 * 
	 * @throws StoreException
	 *         If the Sail encountered an error or unexpected internal state.
	 */
	@Override
//	protected void shutDownInternal() throws StoreException {
	protected void shutDownInternal() throws SailException {
		for (Repository rep : this.members) {
//			rep.shutDown();
			try {
				rep.shutDown();
			} catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
	}

	/**
	 * Returns a wrapper for the Sail connections of the federation members.
	 * 
	 * TODO: currently opens connections to *all* federation members,
	 *       should better use lazy initialization of the connections.
	 *       
	 * @return the Sail connection wrapper.
	 */
	@Override
//	protected SailConnection getConnectionInternal() throws StoreException {
	protected SailConnection getConnectionInternal() throws SailException {
		
		if (!this.initialized)
			throw new IllegalStateException("Sail has not been initialized.");
		
		return new FederationSailConnection(this);
	}

	// SESAME 2.3.2 ============================================================
	
	private final ValueFactory vf = new ValueFactoryImpl();
	
	@Override
	public ValueFactory getValueFactory() {
		return vf;
	}

	@Override
	public boolean isWritable() throws SailException {
		return false;
	}
	
	// SESAME 3.0 ==============================================================
	
//	private final URIFactory uf = new URIFactoryImpl();
//	private final LiteralFactory lf = new LiteralFactoryImpl();

//	/**
//	 * Gets a LiteralFactory object that can be used to create Sail-specific
//	 * literal objects.
//	 * 
//	 * @return a LiteralFactory object for this Sail object.
//	 */
//	@Override
//	public LiteralFactory getLiteralFactory() {
//		return lf;
//	}
//
//	/**
//	 * Gets a URIFactory object that can be used to create Sail-specific URI
//	 * objects.
//	 * 
//	 * @return a URIFactory object for this Sail object.
//	 */
//	@Override
//	public URIFactory getURIFactory() {
//		return uf;
//	}

}