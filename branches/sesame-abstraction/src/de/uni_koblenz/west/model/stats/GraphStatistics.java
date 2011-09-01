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

import java.io.IOException;

/**
 * Statistical information about graph data. 
 * @author goerlitz
 *
 * @param <T>
 */
public interface GraphStatistics<T extends Comparable<T>> {
	
//	public double getCardinality(P triplePattern);
	
	
	
	public double getSelectivity(T[] terms);
	
	public long getGraphCardinality();
	
	public void serialize(StatExporter<T> serializer) throws IOException;
	
	
//	public DataStatistics<T> getPredicateStat();
	
//	public void setPredCounts(CountStatistics<T> psCount, CountStatistics<T> poCount);
}
