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

import java.util.Properties;

import de.uni_koblenz.west.federation.helpers.Format;

/**
 * Statistics which assume a uniform distribution of elements in a collection.
 * The selectivity estimate is the same average for all elements.
 * 
 * @author Olaf Goerlitz
 *
 * @param <T> the element type.
 */
public class AverageStatistics<T extends Comparable<T>> implements DataStatistics<T> {
	
	private double avgSelectivity;
	private long elementCount;

	/**
	 * Create new average statistics for the given number of elements.
	 *  
	 * @param elementCount the number of elements.
	 */
	public AverageStatistics(long elementCount) {
		this.elementCount = elementCount;
		this.avgSelectivity = 1 / (double) this.elementCount;
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public Properties getProperties() {
		Properties props = new Properties();
		props.setProperty("TYPE", "AVG");
		props.setProperty("ELEMENTS", Long.toString(size()));
		props.setProperty("SELECTIVITY", Format.d(this.avgSelectivity, 2));
		return props;
	}
	
	/**
	 * Returns the number of elements covered by the statistics.
	 * 
	 * @return the number of elements.
	 */
	@Override
	public long size() {
		return this.elementCount;
	}
	
	/**
	 * Returns the average selectivity of an element in the collection.
	 * Assumes an uniform distribution of the elements in the collection,
	 * i.e. the selectivity is the same for all elements: 1 / #elements.
	 * 
	 * @param element the element.
	 * @return the average selectivity.
	 */
	@Override
	public double selectivity(T element) {
		return this.avgSelectivity;
	}
	
	// -------------------------------------------------------------------------
	
	public double getCount(T item) {
		throw new UnsupportedOperationException("not supported");
	}
	
	@Override
	public void export(StatExporter<T> exporter) {
		// nothing to do
	}

	@Override
	public void parse(ItemSerializer<T> serializer, String data) {
//		throw new UnsupportedOperationException("nothing is serialized.");
	}

}
