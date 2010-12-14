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
package de.uni_koblenz.west.optimizer.rdf;

import java.net.URI;
import java.util.Set;

import de.uni_koblenz.west.statistics.RDFValue;

/**
 * Adapter methods for mapping a generic model types to its concrete instances.
 * 
 * @author Olaf Goerlitz
 *
 * @param <P> the triple pattern type.
 * @param <F> the filter type.
 */
public interface ModelAdapter<P, F> {
	
	/**
	 * Returns the value of a bound subject or null if not bound. 
	 * @param pattern the pattern
	 * @return the subject binding
	 */
	public URI getSBinding(P pattern);

	/**
	 * Returns the value of a bound predicate or null if not bound. 
	 * @param pattern the pattern
	 * @return the predicate binding
	 */
	public URI getPBinding(P pattern);
	
	/**
	 * Returns the value of a bound object or null if not bound. 
	 * @param pattern the pattern
	 * @return the object binding
	 */
	public RDFValue getOBinding(P pattern);
	
	/**
	 * Returns the unbound variables of a filter.
	 * 
	 * @param filter the filter to examine.
	 * @return the unbound variables of the filter.
	 */
	public Set<String> getFilterVars(F filter);
	
	/**
	 * Returns the unbound variables of a triple pattern.
	 * 
	 * @param pattern the triple pattern to examine.
	 * @return the unbound variables of the triple pattern.
	 */
	public Set<String> getPatternVars(P pattern);
	
	public String getVarName(P pattern, int triplePos);
	
	/**
	 * Returns the bound variables (constant terms) of a triple pattern.
	 * 
	 * @param pattern the triple pattern to examine.
	 * @return the bound variables (constants) of the triple pattern.
	 */
	public String[] getPatternConstants(P pattern);
	
	public int getVarPosition(P pattern, String varName);
	
	/**
	 * Returns the SPARQL representation of a triple pattern.
	 * 
	 * @param pattern the triple pattern to convert.
	 * @return the SPARQL representation of the triple pattern.
	 */
	public String toSparqlPattern(P pattern);

	/**
	 * Returns the SPARQL representation of a filter.
	 * 
	 * @param filter the filter to convert.
	 * @return the SPARQL representation of the filter.
	 */
	public String toSparqlFilter(F filter);
	
	/**
	 * Returns the SPARQL basic graph pattern representation for the operator.
	 * 
	 * @param operator the operator to convert.
	 * @return the SPARQL BGP representation of the operator.
	 */
	public String toSparqlBGP(BGPOperator<P, F> operator);

}
