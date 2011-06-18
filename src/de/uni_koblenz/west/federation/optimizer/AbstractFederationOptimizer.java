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
package de.uni_koblenz.west.federation.optimizer;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.federation.helpers.OperatorTreePrinter;
import de.uni_koblenz.west.federation.model.BasicGraphPatternExtractor;

/**
 * Base functionality for federated query optimizers
 * 
 * @author Olaf Goerlitz
 */
public abstract class AbstractFederationOptimizer implements QueryOptimizer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFederationOptimizer.class);
	
	public abstract void optimizeBGP(TupleExpr query);

	@Override
	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {  // Sesame 2
		
		// collect all basic graph patterns
		for (TupleExpr bgp : BasicGraphPatternExtractor.process(query)) {
			
			if (LOGGER.isDebugEnabled())
				LOGGER.warn("BGP:\n" + OperatorTreePrinter.print(bgp));	
			
			// select sources for basic graph pattern

			// a single statement pattern needs no optimization
			if (bgp instanceof StatementPattern)
				continue;

			optimizeBGP(bgp);
		}
		
	}

}
