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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of an Equi Depth Histogram.
 * 
 * @author Olaf Goerlitz
 *
 * @param <T> the element type.
 */
public class EquiDepthHistogram<T extends Comparable<T>> implements DataStatistics<T> {
	
	private long elementCount;
	private int bins;
	private double s;
	private List<T> borders = new ArrayList<T>();
	private Map<T, Long> borderMap = new HashMap<T, Long>();
	
	/**
	 * Creates a new histogram with the given number of bins and
	 * supplied element counts.
	 * 
	 * @param bins the number of histogram bins.
	 * @param elementMap the element counts.
	 */
	public EquiDepthHistogram(int bins, Map<T, Long> elementMap) {
		
		if (elementMap == null || elementMap.size() == 0)
			throw new IllegalArgumentException("element map is null/empty");
		
		this.elementCount = elementMap.size();
		this.bins = bins;
		
		// sort order is item name
		ArrayList<T> keyList = new ArrayList<T>(elementMap.keySet());
		Collections.sort(keyList);
		
		// get joint count
		long count = 0;
		for (T key : keyList)
			count += elementMap.get(key);
		
		if (bins > count)
			throw new IllegalArgumentException("more bins than elements");
		
		double binRange = count / (double) bins;
		double nextRange = 0;
		long position = 0;
		Iterator<T> it = keyList.iterator();
		
		while (it.hasNext()) {
			T key = it.next();
			position += elementMap.get(key);
			while (position > nextRange) {
				borders.add(key);
				borderMap.put(key, (long) nextRange);
				nextRange += binRange;
			}
		}
		
		System.out.println("created equi-depth histogram: " + borders.size());
		for (T key : borders) {
			Long value = borderMap.get(key);
			Long size = elementMap.get(key);
			System.out.println(value + ": " + key + "=" + size);
		}
	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public Properties getProperties() {
		Properties props = new Properties();
		props.setProperty("TYPE", "EQUI_DEPTH");
		props.setProperty("ELEMENTS", Long.toString(size()));
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
	
	@Override
	public double selectivity(T term) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// -------------------------------------------------------------------------

	@Override
	public void export(StatExporter<T> exporter) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void parse(ItemSerializer<T> serializer, String data) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	public double getCount(T item) {
		throw new UnsupportedOperationException("not supported");
	}

}
