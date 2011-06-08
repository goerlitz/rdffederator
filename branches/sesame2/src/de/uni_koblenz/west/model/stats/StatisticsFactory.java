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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.statistics.parser.RDFCountParser;

/**
 * Handling graph statistics: creation, reading, and writing.
 * 
 * @author Olaf Goerlitz
 */
public class StatisticsFactory {
	
	static final Logger LOGGER = LoggerFactory.getLogger(StatisticsFactory.class);
	
	static final boolean PREDICATE_DETAILS = false;
	
	private StringSerializer serializer = new StringSerializer();

	/**
	 * Extract statistics from a data dump.
	 * 
	 * @param file the file containing the data dump.
	 * @return the extracted statistics.
	 * @throws IOException
	 */
	public GraphStatistics<String> analyseRDFDump(File file) throws IOException {

		// sanity checks
		if (file == null || !file.exists())
			throw new FileNotFoundException("file not found: " + file);
		if (!file.isFile() || !file.canRead())
			throw new IOException("can not read file: " + file);

		// initialize parser
		RDFFormat format = Rio.getParserFormatForFileName(file.getName());
		if (format == null) {
			throw new IOException("unknown RDF format: " + file);
		}

		RDFCountParser counter = new RDFCountParser(PREDICATE_DETAILS);
		RDFParser parser = Rio.createParser(format);
		parser.setRDFHandler(counter);
		try {
			// first pass
			parser.parse(new FileInputStream(file), "");
			
			// more passes?
			while (counter.nextPass()) {
				parser.parse(new FileInputStream(file), "");
			}
			
		} catch (RDFParseException e) {
			throw new IOException("error parsing " + file, e);
		} catch (RDFHandlerException e) {
			throw new IOException("error processing " + file, e);
		}

		return counter.getStatistics();
	}
	
	/**
	 * Import statistics from a file.
	 * 
	 * @param file the import file.
	 * @return the imported statistics.
	 */
	public GraphStatistics<String> readFromFile(File file) throws IOException {
		
		// sanity checks
		if (file == null || !file.exists())
			throw new FileNotFoundException("file not found: " + file);
		if (!file.isFile() || !file.canRead())
			throw new IOException("file is not readable: " + file);
		
		BufferedReader fr = new BufferedReader(new FileReader(file));
		
		ArrayList<DataStatistics<String>> list = new ArrayList<DataStatistics<String>>();
		DataStatistics<String> stat = null;
		long graphSize = 0;
		long subjects = 0;
		long objects = 0;
		
		String data;
		while ((data = fr.readLine()) != null) {
			
			// handle signatures of individual statistics
			if (data.startsWith("#")) {
				
				Properties params = parseParams(data.substring(1));
				
				// get total graph size first
				if (graphSize == 0) {
					try {
						graphSize = Long.parseLong(params.getProperty("TRIPLES"));
						subjects = Long.parseLong(params.getProperty("SUBJECTS"));
						objects = Long.parseLong(params.getProperty("OBJECTS"));
					} catch (NumberFormatException e) {
						throw new IOException("expected graph size. " + e);
					}
					continue;
				}
				
				String type = params.getProperty("TYPE");
				
				if ("AVG".equals(type)) {
					Long count = Long.parseLong(params.getProperty("COUNT"));
					stat = new AverageStatistics<String>(count);
				} else
				if ("COUNT".equals(type)) {
//					String id = params.getProperty("ID");
//					Long count = (long) 0;
//					if (id.equals("SUBJECT"))
//						count = subjects;
//					if (id.equals("OBJECT"))
//						count = objects;
//					stat = new AverageStatistics<String>(count);
					stat = new CountStatistics<String>();
				} else 
				if ("P-COUNT".equals(type)) {
					System.out.println("p-count");
					stat = new PredicateStatistics<String>();
				} else
					throw new IOException("unknown type: " + type);
				list.add(stat);
				continue;
			}
			
			// otherwise, items counts are read.
			stat.parse(serializer, data);
		}
		
		fr.close();
		
		return new RDFStatistics(graphSize, list.get(0), list.get(1), list.get(2));
	}
	
	/**
	 * Write statistics to a file.
	 * 
	 * @param file the export file.
	 * @param overwrite whether overwriting the file is allowed.
	 * @param stats the statistics to export.
	 * @throws IOException
	 */
	public void writeToFile(File file, boolean overwrite, GraphStatistics<String> stats) throws IOException {
		
		// sanity checks
		if (file == null)
			throw new FileNotFoundException("file must not be null.");
		if (file.exists() && !overwrite)
			throw new IOException("file exists, overwriting disabled: " + file);
		if (file.exists() && !file.canWrite())
			throw new IOException("can not overwrite file: " + file);
		
		PrintWriter pr = new PrintWriter(file);
		stats.serialize(new StatExporter<String>(pr, serializer));
		pr.close();
	}
	
	// -------------------------------------------------------------------------

	/**
	 * Parse comma-separated key:value parameters.
	 * 
	 * @param data the string containing the parameters.
	 * @return the parsed key:value properties.
	 */
	private Properties parseParams(String data) {
//		Map<String, String> params = new HashMap<String, String>();
		Properties props = new Properties();
		
		for (String part : data.split(",")) {
			int pos = part.indexOf("=");
			if (pos > 0)
				props.setProperty(part.substring(0, pos), part.substring(pos+1));
			else
				props.setProperty(part, null);
		}
		return props;
	}
	
	// -------------------------------------------------------------------------
	
	private static class StringSerializer implements ItemSerializer<String> {

		@Override
		public String parse(String encodedString) {
			return encodedString;
		}

		@Override
		public String serialize(String object) {
			return object;
		}
		
	}
	
	// -------------------------------------------------------------------------
	
	public static void main(String[] args) throws Exception {
		
//		String DATA_FILE = "/home/goerlitz/datasets/sp2b_50k.nt";
		String DATA_FILE = "/home/goerlitz/datasets/linkedmdb20100129.nt";
		String STAT_FILE = "/home/goerlitz/datasets/sp2b_50k.stats";
		
		if (args.length != 2) {
			LOGGER.info("using default input file: " + DATA_FILE);
			args = new String[] {DATA_FILE, STAT_FILE};
		}
		
//		// check parameters
//		if (args.length < 1) {
//			System.err.println("Please specify a RDF file.");
//			return;
//		}
		
		StatisticsFactory fac = new StatisticsFactory();
		GraphStatistics<String> stats = fac.analyseRDFDump(new File(DATA_FILE));
//		fac.writeToFile(new File(STAT_FILE), true, stats);
		
//		stats = fac.readFromFile(new File(STAT_FILE));
//		fac.writeToFile(new File(STAT_FILE + "_copy"), true, stats);
	}

}
