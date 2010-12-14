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

import java.io.PrintWriter;
import java.util.Properties;

/**
 * Exports statistics to a file.
 * 
 * @author Olaf Goerlitz
 *
 * @param <T> the element type
 */
public class StatExporter<T extends Comparable<T>> {
	
	private PrintWriter out;
	private ItemSerializer<T> serializer;
	
	public StatExporter(PrintWriter out, ItemSerializer<T> serializer) {
		this.out = out;
		this.serializer = serializer;
	}
	
	public void writeStatistics(DataStatistics<T> stats, String id) {
		Properties props = stats.getProperties();
		props.setProperty("ID", id);
		
		StringBuffer buffer = new StringBuffer("#");
		for (String name : props.stringPropertyNames()) {
			buffer.append(name).append("=").append(props.getProperty(name));
			buffer.append(",");
		}
		buffer.setLength(buffer.length() - 1);
		out.println(buffer.toString());
		
		stats.export(this);
	}
	
	public void writeSignature(String data) {
		out.println("#" + data);
	}
	
	public void writeln(T key, Object value) {
		out.println(serializer.serialize(key) + "\t" + value);
	}
}
