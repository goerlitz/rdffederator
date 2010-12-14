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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Multiple exact counts for a collection of elements.
 * Each element is associated with a fixed number of counts, usually
 * representing the frequency of occurrence in the collection.
 * The first dimension is the major statistic for selectivity estimation.
 * 
 * @author Olaf Goerlitz
 * 
 * @param <T> the element type.
 */
//public class PredicateStatistics<T extends Comparable<T>, S extends DataStatistics<T>>
public class PredicateStatistics<T extends Comparable<T>> implements DataStatistics<T> {
	
	private long sumCounts = -1;

	class Triple<U, V, W> {
		
		public Triple(U first, V second, W third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
		
		U first;
		V second;
		W third;
	}
	
//	private int size;
//	private Map<T, Triple<Long, S, S>> itemMaps = new HashMap<T, Triple<Long,S,S>>();
	private Map<T, Triple<Long, DataStatistics<T>, DataStatistics<T>>> itemMaps = new HashMap<T, Triple<Long,DataStatistics<T>,DataStatistics<T>>>();
	
	/**
	 * Create a new empty collection of element counts.
	 */
	public PredicateStatistics() { }
	
	// -------------------------------------------------------------------------
	
	@Override
	public Properties getProperties() {
		Properties props = new Properties();
		props.setProperty("TYPE", "P-COUNT");
		props.setProperty("ELEMENTS", Long.toString(size()));
		props.setProperty("COUNTSUM", Long.toString(getCountSum()));
		return props;
	}
	
	@Override
	public long size() {
		return this.itemMaps.size();
	}
	
	@Override
	public double selectivity(T element) {
		Long value = itemMaps.get(element).first;
		
		if (value == null)
			throw new IllegalArgumentException("not in collection: " + element);
		
		return value / (double) this.getCountSum();
	}
	
	@Override
	public void export(StatExporter<T> exporter) {
		for (T key : sortByKey()) {
			Triple<Long, DataStatistics<T>, DataStatistics<T>> triple = itemMaps.get(key);
			long size1 = triple.second.size();
			long size2 = triple.third.size();
			// TODO: this is ugly
			exporter.writeln(key, triple.first + "\t" + size1 + "\t" + size2);
		}
	}
	
	@Override
	public void parse(ItemSerializer<T> serializer, String data) {
		String[] parts = data.split("\t");
		long count = Long.parseLong(parts[1]);
		AverageStatistics<T> psStat = new AverageStatistics<T>(Long.parseLong(parts[2]));
		AverageStatistics<T> poStat = new AverageStatistics<T>(Long.parseLong(parts[3]));
		set(serializer.parse(parts[0]), count, psStat, poStat);
	}
	
	// -------------------------------------------------------------------------
	
	public void set(T element, long count, DataStatistics<T> psStat, DataStatistics<T> poStat) {
		this.itemMaps.put(element, new Triple<Long, DataStatistics<T>, DataStatistics<T>>(count, psStat, poStat));
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the sum of all element counts in the collection.
	 * 
	 * @return the sum of all counts.
	 */
	private long getCountSum() {
		if (this.sumCounts < 0)
			for (T t : itemMaps.keySet())
				sumCounts += itemMaps.get(t).first;
		return this.sumCounts;
	}
	
	/**
	 * Sort the map by keys.
	 * 
	 * @return the sorted key list.
	 */
	private List<T> sortByKey() {
		List<T> keyOrder = new ArrayList<T>(itemMaps.keySet());
		Collections.sort(keyOrder);
		return keyOrder;
	}

}
