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
package de.uni_koblenz.west.model.stats;

import java.util.List;
import java.util.Properties;

/**
 * Statistics for a collection of elements.
 * 
 * @author Olaf Goerlitz
 *
 * @param <T> the element type.
 */
public interface DataStatistics<T extends Comparable<T>> {
	
	/**
	 * Returns the properties describing the statistics.
	 * 
	 * @return the properties describing the statistics.
	 */
	public Properties getProperties();
	
	/**
	 * Returns the number of elements covered by the statistics.
	 * 
	 * @return the number of elements.
	 */
	public long size();
	
	/**
	 * Returns the selectivity of the specified element in the collection.
	 * 
	 * @param element the element.
	 * @return the element's selectivity.
	 */
	public double selectivity(T element);
	
	
	public void export(StatExporter<T> exporter);
	
	public void parse(ItemSerializer<T> serializer, String data);
	
}
