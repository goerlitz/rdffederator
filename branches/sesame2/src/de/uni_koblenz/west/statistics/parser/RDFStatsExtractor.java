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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.statistics.model.CountMap;
import de.uni_koblenz.west.statistics.model.StatMap;

/**
 * Collect statistical data from RDF input.
 * 
 * @author Olaf Goerlitz
 */
//public class RDFStatsExtractor extends RDFHandlerBase implements MultiPassParser {
public class RDFStatsExtractor extends RDFHandlerBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RDFStatsExtractor.class);

	// general counts for triples, subjects, predicates, and objects
	private long tripleCount = 0;
//	private int pass = 0;
//	private boolean multiPass;
	
	private Set<URI> datatypes = new HashSet<URI>();
	private Set<Resource> entities = new HashSet<Resource>();
	private Set<Resource> subjects = new HashSet<Resource>();
	private Set<Value> objects = new HashSet<Value>();
	
	private Map<URI, Set<Resource>> distPredSubjects = new HashMap<URI, Set<Resource>>();
	private Map<URI, Set<Value>> distPredObjects = new HashMap<URI, Set<Value>>();
	
	private CountMap<URI> typeCounts = new CountMap<URI>();
	
	// predicate -> datatype -> count stats
	private StatMap<URI> predStats = new StatMap<URI>();
	
	// comparator for URIs, BNodes, Literals
	private static final Comparator<Value> VAL_COMP = new Comparator<Value>() {
		@Override public int compare(Value val1, Value val2) {
			return val1.stringValue().compareTo(val2.stringValue());
		}
	};
	
//	public RDFStatsExtractor(boolean multiPass) {
//		
//	}
//	
//	// -------------------------------------------------------------------------
//	
//	public boolean hasFinished() {
//		return pass != 0;
////		if (pass == 0 || distPredSubjects.size() < )
//	}
//	
//	public void nextPass() {
//		
//	}
	
	// -------------------------------------------------------------------------
	
	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		
		Resource subject = st.getSubject();
		URI predicate = st.getPredicate();
		Value object = st.getObject();
		
		// count distinct subjects and objects
		subjects.add(subject);
		objects.add(object);
		
		// store entity types (SELECT * WHERE {[] a ?type}
		if (RDF.TYPE.equals(predicate)) {
			entities.add(subject);
			typeCounts.add((URI) object);
		}
		
		// count distinct subjects and objects per predicate
		Set<Resource> distS = distPredSubjects.get(predicate);
		if (distS == null) {
			distS = new HashSet<Resource>();
			distPredSubjects.put(predicate, distS);
		}
		Set<Value> distO = distPredObjects.get(predicate);
		if (distO == null) {
			distO = new HashSet<Value>();
			distPredObjects.put(predicate, distO);
		}
		distS.add(subject);
		distO.add(object);
		
		// count frequency of object datatypes - stores URI/BNode class as type too
		if (object instanceof URI) {
			predStats.add(predicate, URI.class, (URI) object, VAL_COMP);
		} else if (object instanceof BNode) {
			predStats.add(predicate, BNode.class, (BNode) object, VAL_COMP);
		} else if (object instanceof Literal) {
			mapDatatype(predicate, (Literal) object, predStats);
		} else
			throw new IllegalArgumentException("illegal object type: " + object.getClass());
		
		tripleCount++;
	}

	// -------------------------------------------------------------------------
	
	/**
	 * Maps the XML Schema data types to java classes.
	 * 
	 * @param predicate the considered predicate.
	 * @param literal the considered literal.
	 * @param stats the statistics to update.
	 */
	private void mapDatatype(URI predicate, Literal literal, StatMap<URI> stats) {
		URI datatype = literal.getDatatype();
		
		if (datatype == null || XMLSchema.STRING.equals(datatype)) {
			stats.add(predicate, String.class, literal.stringValue());
		} else if (XMLSchema.DECIMAL.equals(datatype)) {
			stats.add(predicate, BigDecimal.class, literal.decimalValue());
		} else if (XMLSchema.INTEGER.equals(datatype)) {
			stats.add(predicate, BigInteger.class, literal.integerValue());
		} else if (XMLSchema.BOOLEAN.equals(datatype)) {
			stats.add(predicate, Boolean.class, literal.booleanValue());
		} else if (XMLSchema.SHORT.equals(datatype)) {
			stats.add(predicate, Short.class, literal.shortValue());
		} else if (XMLSchema.INT.equals(datatype)) {
			stats.add(predicate, Integer.class, literal.intValue());
		} else if (XMLSchema.LONG.equals(datatype)) {
			stats.add(predicate, Long.class, literal.longValue());
		} else if (XMLSchema.FLOAT.equals(datatype)) {
			stats.add(predicate, Float.class, literal.floatValue());
		} else if (XMLSchema.DOUBLE.equals(datatype)) {
			stats.add(predicate, Double.class, literal.doubleValue());
		// Calendar data type, needs to be comparable. Support for XML Schema
		// dateTime, time, date, gYearMonth, gMonthDay, gYear, gMonth or gDay
//		} else if (XMLSchema.DATETIME.equals(datatype) ||
//				XMLSchema.DATE.equals(datatype) ||
//				XMLSchema.TIME.equals(datatype) ||
//				XMLSchema.GDAY.equals(datatype) ||
//				XMLSchema.GMONTH.equals(datatype) ||
//				XMLSchema.GMONTHDAY.equals(datatype) ||
//				XMLSchema.GYEAR.equals(datatype) ||
//				XMLSchema.GYEARMONTH.equals(datatype)) {
//				stats.add(predicate, XMLGregorianCalendar.class, literal.calendarValue());
		} else {
			// memorize unrecognized data types
			if (!datatypes.contains(datatype)) {
				datatypes.add(datatype);
				LOGGER.warn("unrecognized datatype: " + datatype);
			}
			stats.add(predicate, Literal.class, literal, VAL_COMP);
		}
	}
	
	public long getTriples() {
		return this.tripleCount;
	}
	
	public int getProperties() {
		return this.predStats.keySet().size();
	}
	
	public int getDistinctSubjects() {
		return this.subjects.size();
	}
	
	public int getDistinctSubjects(URI predicate) {
		return this.distPredSubjects.get(predicate).size();
	}
	
	public int getDistinctObjects(URI predicate) {
		return this.distPredObjects.get(predicate).size();
	}
	
	public List<Value> getSortedObjects() {
		List<Value> objList = new ArrayList<Value>(objects);
		Collections.sort(objList, new Comparator<Value>() {
			@Override public int compare(Value val1, Value val2) {
				return val1.stringValue().compareTo(val2.stringValue());
			}
		});
		return objList;
	}
	
	public List<Resource> getSortedSubjects() {
		List<Resource> subjList = new ArrayList<Resource>(subjects);
		Collections.sort(subjList, new Comparator<Value>() {
			@Override public int compare(Value val1, Value val2) {
				return val1.stringValue().compareTo(val2.stringValue());
			}
		});
		return subjList;
	}
	
	public int getDistinctObjects() {
		return this.objects.size();
	}
	
	public int getEntities() {
		return this.entities.size();
	}
	
	public long getPredicateCount(URI uri) {
		return this.predStats.getCount(uri);
	}
	
	public long getTypeCount(URI uri) {
		return this.typeCounts.getCount(uri);
	}
	
	public Set<URI> predKeySet() {
		return this.predStats.keySet();
	}
	
	public Set<URI> typeKeySet() {
		return this.typeCounts.keySet();
	}
	
//	private void printResults() {
//		if (LOGGER.isInfoEnabled()) {
//			
//			List<Class<?>> classes = new ArrayList<Class<?>>(predStats.getClasses());
//			Collections.sort(classes, new Comparator<Class<?>>() {
//				@Override public int compare(Class<?> o1, Class<?> o2) {
//					return o1.getName().compareTo(o2.getName());
//				}
//			});
//			
//			// print stat details per class
//			for (Class<?> clazz : classes) {
//				Map<URI, ? extends StatEntry<?>> stats = predStats.getStats(clazz);
//				LOGGER.info(clazz.toString() + " [" + stats.size() + " elements]");
//				
//				List<URI> keys = new ArrayList<URI>(stats.keySet());
//				Collections.sort(keys, VAL_COMP);
//				for (URI key : keys) {
//					LOGGER.info(key + " [" + clazz + "] " + stats.get(key));
//				}
//			}
//			
//			// print overall counts per class
//			for (Class<?> clazz : classes) {
//				Map<URI, ? extends StatEntry<?>> stats = predStats.getStats(clazz);
//				int frequency = 0;
//				for (URI uri : stats.keySet()) {
//					frequency += stats.get(uri).getCount();
//				}
//				LOGGER.info(clazz.toString() + ", " + stats.size() + " elements, " + frequency + " frequency");
//			}
//			
//			LOGGER.info("#T=" + tripleCount);
//		}
//	}
	
}
