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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.uni_koblenz.west.federation.helpers.Format;

/**
 * Exact counts for a collection of elements.
 * Each element is associated with a count representing its frequency
 * of occurrence in the collection. 
 * 
 * @author Olaf Goerlitz
 *
 * @param <T> the element type.
 */
public class CountStatistics<T extends Comparable<T>> implements DataStatistics<T> {
	
	private Map<T, Long> itemMap = new HashMap<T, Long>();
	private List<T> keyOrder = null;
	private long sumCounts = -1;
	
	/**
	 * Create a new empty collection of element counts.
	 */
	public CountStatistics() { }
	
	// -------------------------------------------------------------------------
	
	@Override
	public Properties getProperties() {
		Properties props = new Properties();
		props.setProperty("TYPE", "COUNT");
		props.setProperty("ELEMENTS", Long.toString(size()));
		props.setProperty("COUNTSUM", Long.toString(getCountSum()));
		return props;
	}
	
	/**
	 * Returns the number of elements covered by the statistics.
	 * 
	 * @return the number of elements.
	 */
	@Override
	public long size() {
		return this.itemMap.size();
	}
	
	/**
	 * Returns the exact selectivity of the specified element in the collection.
	 * The exact selectivity is the count of the specified element divided by
	 * the sum of all element counts.
	 * 
	 * @param element the element.
	 * @return the element's selectivity.
	 */
	@Override
	public double selectivity(T element) {
		Long value = itemMap.get(element);
		
		if (value == null)
			throw new IllegalArgumentException("not in collection: " + element);
		
		return value / (double) this.getCountSum();
	}
	
	public void export(StatExporter<T> exporter) {
		sortByKey();
		for (T key : keyOrder) {
			exporter.writeln(key, itemMap.get(key));
		}
	}
	
	public void parse(ItemSerializer<T> serializer, String data) {
		String[] pair = data.split("\t");
		set(serializer.parse(pair[0]), Long.parseLong(pair[1]));
	}
	
	// -------------------------------------------------------------------------
	
	public long getCount(T element) {
		Long value = itemMap.get(element);
		
		if (value == null)
			throw new IllegalArgumentException("not in collection: " + element);
		
		return value;
	}
	
	/**
	 * Returns the sum of all element counts in the collection.
	 * 
	 * @return the sum of all counts.
	 */
	private long getCountSum() {
		if (this.sumCounts < 0)
			for (T t : itemMap.keySet())
				sumCounts += itemMap.get(t);
		return this.sumCounts;
	}
	
	// -------------------------------------------------------------------------

	public List<T> getElements() {
		
		// use key order if it's presents and up-to-date 
		if (keyOrder != null && keyOrder.size() == itemMap.keySet().size())
			return keyOrder;
		else
			return new ArrayList<T>(this.itemMap.keySet());
	}
	
	/**
	 * Increment the element counter.
	 * 
	 * @param element the element for which to increment the counter. 
	 */
	public void inc(T element) {
		Long count = itemMap.get(element);
		if (count == null)
			itemMap.put(element, 1l);
		else
			itemMap.put(element, count + 1);
		this.sumCounts++;
	}
	
	public void set(T element, long count) {
		Long itemCount = itemMap.get(element);
		if (itemCount != null)
			this.sumCounts -= itemCount;
		itemMap.put(element, count);
		this.sumCounts += count;
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sort the map by keys.
	 * 
	 * @return the sorted map.
	 */
	public CountStatistics<T> sortByKey() {
		keyOrder = new ArrayList<T>(itemMap.keySet());
		Collections.sort(keyOrder);
		return this;
	}
	
	/**
	 * Sort the map by value.
	 * 
	 * @return the sorted map.
	 */
	public CountStatistics<T> sortByValue() {
		keyOrder = new ArrayList<T>(itemMap.keySet());
		Collections.sort(keyOrder, new ValueComparator());
		return this;
	}
	
	/**
	 * Print all key:count pair of the map. 
	 */
	public void print() {
		Collection<T> keys = itemMap.keySet();
		
		// use key order if it's presents and up-to-date 
		if (keyOrder != null && keyOrder.size() == itemMap.keySet().size())
			keys = keyOrder;
		
		for (T key : keys) {
			System.out.println("'" + key + "': " + itemMap.get(key));
		}
	}
	
	/**
	 * Comparator for value comparison.
	 */
	private class ValueComparator implements Comparator<T> {
		@Override
		public int compare(T o1, T o2) {
			return itemMap.get(o1).compareTo(itemMap.get(o2));
		}
	}
	
	public String metaStatsToString(String name) {
		
		long min = Long.MAX_VALUE;
		long max = 0;
		double mean = this.getCountSum() / (double) this.size();
		double deviation = 0;
		
		for (Long value : itemMap.values()) {
			if (min > value) min = value;
			if (max < value) max = value;
			double diff = mean - value;
			deviation += diff * diff;
		}
		
		deviation = Math.sqrt(deviation / (double) size());
		
//		if (mean < deviation) {
//			for (String key : counts.keySet()) {
//				if (counts.get(key) > (mean + deviation))
//					System.out.println(">dev : " + key + " - " + counts.get(key));
//			}
//		}
		
		return "#" + name + ": " + size() +
				     ", minFreq=" + min + ", maxFreq=" + max + 
				     ", meanFreq=" + Format.d(mean, 2) + 
				     ", deviation=" + Format.d(deviation, 2) + 
				     ", sel=" + Format.d(1 / (double) size(), 5);
	}
}
