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
package de.uni_koblenz.west.statistics.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFGeoCoordParser extends RDFHandlerBase {
	
	static final Logger LOGGER = LoggerFactory.getLogger(RDFGeoCoordParser.class);
	
	static final String RDF_FILE = "/home/goerlitz/Downloads/geo_coordinates_en.nt";
	
	static final ValueFactory vf = new ValueFactoryImpl();
	
	static final URI GEO_LAT = vf.createURI("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
	static final URI GEO_LNG = vf.createURI("http://www.w3.org/2003/01/geo/wgs84_pos#long");
	
	static final int[] longHistogram = new int[360];
	
	public static void main(String[] args) {
		
		if (args.length == 0) {
			LOGGER.info("using default input file: " + RDF_FILE);
			args = new String[] {RDF_FILE};
		} else {
			LOGGER.info("reading from file: " + args[0]);
		}
		
		String filename = args[0];
		
		// initialize parser
		RDFFormat format = Rio.getParserFormatForFileName(filename);
		if (format == null) {
			throw new UnsupportedOperationException("Unsupported RDF format: " + filename);
		}
		RDFParser parser = Rio.createParser(format);
		parser.setRDFHandler(new RDFGeoCoordParser());
		try {
			long start = System.currentTimeMillis();
			parser.parse(new FileInputStream(filename), "");
			LOGGER.info("time taken: " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
		} catch (RDFParseException e) {
			e.printStackTrace();
		} catch (RDFHandlerException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < longHistogram.length; i++) {
			System.out.println(((i - 180)) + "\t" + longHistogram[i]);
		}
	}


	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		
		try {
			if (GEO_LNG.equals(st.getPredicate())) {
				Value value = st.getObject();
				double lng = ((Literal) value).doubleValue();
				int bucket = (int) Math.floor(lng) + 180;
				longHistogram[bucket]++;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// only happens if geo:long is +180 (-180 maps to 1st bucket)
		}
		
	}
}
