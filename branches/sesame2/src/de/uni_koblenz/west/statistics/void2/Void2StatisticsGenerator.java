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
package de.uni_koblenz.west.statistics.void2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.ParseLocationListener;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.statistics.parser.RDFStatsExtractor;
import de.uni_koblenz.west.statistics.util.CompactBNodeTurtleWriter;
import de.uni_koblenz.west.vocabulary.VOID2;

/**
 * Generate voiD 2 statistics for the supplied RDF input file.
 * 
 * @author Olaf Goerlitz
 */
public class Void2StatisticsGenerator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Void2StatisticsGenerator.class);
	
	private static final String USAGE = "A properties config file is required.\n"
		+ "mandatory config properties are:\n"
		+ "* dump_file: a (zipped) RDF dump file\n"
		+ "* void_file: the voiD 2 output file\n"
		+ "* sparql_endpoint: URI of the associated SPARQL endpoint";

	private final RDFStatsExtractor voidParser = new RDFStatsExtractor();
	private final ValueFactory vf = ValueFactoryImpl.getInstance();
	private final Comparator<Value> VAL_COMP = new Comparator<Value>() {
		@Override public int compare(Value val1, Value val2) {
			return val1.stringValue().compareTo(val2.stringValue());
		}
	};
	
	private String dumpfile;
	private String voidfile;
	private String endpoint;
	
	/**
	 * Processes a config file.
	 * 
	 * @param configFile the name of the config file. 
	 */
	private void process(String configFile) {
		
		Properties config = readConfig(configFile);
		initConfigParams(config);
		
		Map<String, InputStream> streams = getInputStreams(this.dumpfile);
		
		// read all files
		for (String name : streams.keySet()) {
			
			LOGGER.info("parsing: " + name);
			
			// initialize parser
			RDFFormat format = Rio.getParserFormatForFileName(name);
			if (format == null) {
				throw new UnsupportedOperationException("Unsupported input format: " + name);
			}
			RDFParser parser = Rio.createParser(format);
			parser.setRDFHandler(voidParser);
//			parser.setStopAtFirstError(false);
//			parser.setVerifyData(false);
//			parser.setParseErrorListener(new ErrorHandler());
			parser.setParseLocationListener(new ParseLocationListener() {
				@Override
				public void parseLocationUpdate(int lineNo, int columnNo) {
					if (lineNo % 100000 == 0)
						LOGGER.info("linecount: " + lineNo);
				}
			});
			
			long start = System.currentTimeMillis();
			try {
				parser.parse(streams.get(name), "");
			} catch (RDFParseException e) {
				e.printStackTrace();
			} catch (RDFHandlerException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			LOGGER.info("time taken: " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
		}
		
		writeVoidFile();
//		writeObjects();
//		writeSubjects();
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Read the configuration file.
	 * 
	 * @param file the file to read.
	 * @return the read configuration.
	 */
	private Properties readConfig(String file) {
		Properties config = new Properties();
		
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				config.load(in);
			} catch (IOException e) {
				System.out.println("can not read configuration file: " + file);
				System.exit(1);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException e) {
			System.out.println("configuration file not found: " + file);
			System.exit(1);
		} catch (IOException e) {
			// unable to close file, probably already closed
		}
		return config;
	}
	
	/**
	 * Reads and checks the configuration parameters.
	 * 
	 * @param config the configuration.
	 */
	private void initConfigParams(Properties config) {
		dumpfile = config.getProperty("dump_file");
		if (dumpfile == null) {
			System.out.println("input property 'dump_file' is missing.");
			System.exit(1);
		}
		voidfile = config.getProperty("void_file");
		if (voidfile == null) {
			System.out.println("output property 'void_file' is missing.");
			System.exit(1);
		}
		endpoint = config.getProperty("sparql_endpoint");
		if (endpoint == null) {
			System.out.println("property 'sparql_endpoint' is missing.");
			System.exit(1);
		}
	}
	
	/**
	 * Collects the InputStreams for the files to read.
	 * 
	 * @param file the file to read from
	 * @return a Mapping of files to InputStreams.
	 */
	private Map<String, InputStream> getInputStreams(String file) {
		
		Map<String, InputStream> streams = new HashMap<String, InputStream>();
		
		try {
			if (file.toLowerCase().endsWith(".zip")) {
				ZipFile zf = new ZipFile(file);
				Enumeration<? extends ZipEntry> entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
			    	if (!entry.isDirectory()) {
			    		try {
							streams.put(entry.getName(), zf.getInputStream(entry));
						} catch (Exception e) {
							LOGGER.warn("Can not read zip entry: " + entry.getName(), e);
						}
			    	}
			    }
			} else {
				streams.put(file, new FileInputStream(file));
			}
		} catch (FileNotFoundException e) {
			LOGGER.warn("Unable to file file: " + file, e);
		} catch (IOException e) {
			LOGGER.warn("Can not read zip file: " + file, e);
		}
		return streams;
	}
	
	/**
	 * Converts an enum URI string to a Sesame URI.
	 * 
	 * @param uri the URI string to convert. 
	 * @return the converted Sesame URI.
	 */
	private URI toURI(Enum<?> uri) {
		return vf.createURI(uri.toString());
	}
	
	/**
	 * Writes all distinct object to System.out.
	 */
	private void writeObjects() {
		RDFWriter writer = new NTriplesWriter(System.out);
		try {
			writer.startRDF();
			for (Value val : voidParser.getSortedObjects()) {
				writer.handleStatement(vf.createStatement(RDF.TYPE, RDF.TYPE, val));
			}
			writer.endRDF();
		} catch (RDFHandlerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes all distinct subjects to System.out.
	 */
	private void writeSubjects() {
		RDFWriter writer = new NTriplesWriter(System.out);
		try {
			writer.startRDF();
			for (Resource val : voidParser.getSortedSubjects()) {
				writer.handleStatement(vf.createStatement(val, RDF.TYPE, RDF.TYPE));
			}
			writer.endRDF();
		} catch (RDFHandlerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the voiD 2 output file.
	 */
	private void writeVoidFile() {
		// initialize void writer
		RDFFormat format = Rio.getParserFormatForFileName(this.voidfile);
		RDFWriter writer = null;
		if (format == null) {
			throw new UnsupportedOperationException("Unsupported output format: " + this.voidfile);
		}
		try {
			if (RDFFormat.N3.equals(format) || RDFFormat.TURTLE.equals(format)) {
				writer = new CompactBNodeTurtleWriter(new FileWriter(this.voidfile));
			} else {
				writer = Rio.createWriter(format, new FileWriter(this.voidfile));
			}
		} catch (UnsupportedRDFormatException e) {
			throw new UnsupportedOperationException("Unsupported output format: " + this.voidfile);
		} catch (IOException e) {
			throw new IllegalArgumentException("Can not write to file: " + this.voidfile);
		}
		
		BNode dataset = vf.createBNode();
		
		URI sparqlEndpoint = vf.createURI(this.endpoint);
		Literal triples    = vf.createLiteral(String.valueOf(voidParser.getTriples()), XMLSchema.INTEGER);
		Literal properties = vf.createLiteral(String.valueOf(voidParser.getProperties()), XMLSchema.INTEGER);
		Literal entities   = vf.createLiteral(String.valueOf(voidParser.getEntities()), XMLSchema.INTEGER);
		Literal distinctS  = vf.createLiteral(String.valueOf(voidParser.getDistinctSubjects()), XMLSchema.INTEGER);
		Literal distinctO  = vf.createLiteral(String.valueOf(voidParser.getDistinctObjects()), XMLSchema.INTEGER);
		
		try {
			writer.startRDF();
			
			// add namespaces which will be automatically shortened
			writer.handleNamespace("owl", "http://www.w3.org/2002/07/owl#");
			writer.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
			writer.handleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			writer.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			writer.handleNamespace("skos", "http://www.w3.org/2004/02/skos/core#");
			writer.handleNamespace("foaf", "http://xmlns.com/foaf/0.1/");
			writer.handleNamespace("void", "http://rdfs.org/ns/void#");
			writer.handleNamespace("dbpo", "http://dbpedia.org/ontology/");
			writer.handleNamespace("dbpp", "http://dbpedia.org/property/");
			writer.handleNamespace("dbpedia", "http://dbpedia.org/resource/");
			writer.handleNamespace("swrc", "http://swrc.ontoware.org/ontology#");
			writer.handleNamespace("dcterms", "http://purl.org/dc/terms/");
			writer.handleNamespace("dc", "http://purl.org/dc/elements/1.1/");
			writer.handleNamespace("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
			writer.handleNamespace("gml", "http://www.opengis.net/gml/");
			writer.handleNamespace("georss", "http://www.georss.org/georss/");
			writer.handleNamespace("geonames", "http://www.geonames.org/ontology#");
			writer.handleNamespace("nyt", "http://data.nytimes.com/elements/");
			writer.handleNamespace("cc", "http://creativecommons.org/ns#");
			writer.handleNamespace("bio", "http://bio2rdf.org/ns/bio2rdf#");
			writer.handleNamespace("kegg", "http://bio2rdf.org/ns/kegg#");
			writer.handleNamespace("drugbank", "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/");
			writer.handleNamespace("lmdb", "http://data.linkedmdb.org/resource/movie/");
			writer.handleNamespace("lmdblink", "http://data.linkedmdb.org/resource/oddlinker/");
			writer.handleNamespace("chebi", "http://bio2rdf.org/ns/chebi#");

			// general void information
			writer.handleStatement(vf.createStatement(dataset, RDF.TYPE, toURI(VOID2.Dataset)));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.sparqlEndpoint), sparqlEndpoint));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.triples), triples));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.entities), entities));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.properties), properties));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.distinctSubjects), distinctS));
			writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.distinctObjects), distinctO));
			
			// write predicate statistics
			List<URI> predicates = new ArrayList<URI>(voidParser.predKeySet());
			Collections.sort(predicates, VAL_COMP);
			for (URI p : predicates) {
				BNode propPartition = vf.createBNode();
				Literal count = vf.createLiteral(String.valueOf(voidParser.getPredicateCount(p)));
				distinctS  = vf.createLiteral(String.valueOf(voidParser.getDistinctSubjects(p)), XMLSchema.INTEGER);
				distinctO  = vf.createLiteral(String.valueOf(voidParser.getDistinctObjects(p)), XMLSchema.INTEGER);
				writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.propertyPartition), propPartition));
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.property), p));
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.triples), count));
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.distinctSubjects), distinctS));
				writer.handleStatement(vf.createStatement(propPartition, toURI(VOID2.distinctObjects), distinctO));
			}
			
			// write type statistics
			List<URI> types = new ArrayList<URI>(voidParser.typeKeySet());
			Collections.sort(types, VAL_COMP);
			for (URI uri : types) {
				BNode classPartition = vf.createBNode();
				Literal count = vf.createLiteral(String.valueOf(voidParser.getTypeCount(uri)), XMLSchema.INTEGER);
				writer.handleStatement(vf.createStatement(dataset, toURI(VOID2.classPartition), classPartition));
				writer.handleStatement(vf.createStatement(classPartition, toURI(VOID2.clazz), uri));
				writer.handleStatement(vf.createStatement(classPartition, toURI(VOID2.entities), count));
			}
			
			writer.endRDF();
		} catch (RDFHandlerException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		if (args.length == 0) {
			System.out.println(USAGE);
			System.exit(0);
		}
		
		new Void2StatisticsGenerator().process(args[0]);
	}
	
	// -------------------------------------------------------------------------
	
	class ErrorHandler implements ParseErrorListener {
		
		@Override
		public void warning(String msg, int lineNo, int colNo) {
			LOGGER.warn("WARN: " + msg + "  @" + lineNo + ":" + colNo);
		}
		
		@Override
		public void fatalError(String msg, int lineNo, int colNo) {
			LOGGER.warn("FATAL: " + msg + "  @" + lineNo + ":" + colNo);
		}
		
		@Override
		public void error(String msg, int lineNo, int colNo) {
			LOGGER.warn("ERROR: " + msg + "  @" + lineNo + ":" + colNo);
		}
	}
	
}
