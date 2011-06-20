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
package de.uni_koblenz.west.federation.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import de.uni_koblenz.west.federation.index.Graph;
import de.uni_koblenz.west.statistics.Void2StatsRepository;
import de.uni_koblenz.west.statistics.Void2Statistics;
import de.uni_koblenz.west.vocabulary.RDF;

/**
 * Test the querying of void statistics loaded into a repository.
 * 
 * @author Olaf Goerlitz
 */
public class Void2StatisticsTest {
	
	private static final String[] STAT_FILES = {"void1.n3", "void2.n3"};
	private static final Void2Statistics voidStats = new Void2StatsRepository();
	
	private static URI RDF_TYPE;
	private static URI FOAF_NAME;
	private static URI FOAF_PERSON;
	private static URI GEO_LAT;
	private static URI GML_FEATURE;
	
	static {
		try {
			RDF_TYPE    = new URI(RDF.type.toString());
			FOAF_NAME   = new URI("http://xmlns.com/foaf/0.1/name");
			FOAF_PERSON = new URI("http://xmlns.com/foaf/0.1/Person");
			GEO_LAT     = new URI("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
			GML_FEATURE = new URI("http://www.opengis.net/gml/_Feature");
		} catch (URISyntaxException e) {
			throw new RuntimeException("URI initialization failed" , e);
		}
	}
	
	/**
	 * Load void statistics from local files.
	 */
	@BeforeClass
	public static void setUp() {
		for (String statFile : STAT_FILES) {
			URL url = Void2StatisticsTest.class.getResource(statFile);
			try {
				voidStats.load(url);
			} catch (Exception e) {
				throw new RuntimeException("can not load " + url, e);
			}
		}
	}
	
	/**
	 * Test results of statistics queries returned from the void repository.
	 * @throws URISyntaxException
	 */
	@Test
	public void test() throws URISyntaxException {
		
//		Set<Graph> sources = voidStats.findGraphs(RDF_TYPE.toString());
		Set<Graph> sources = voidStats.findSources(null, RDF_TYPE.toString(), null, false);
		Assert.assertTrue(sources.size() == 2);

//		sources = voidStats.findGraphs(GEO_LAT.toString());
		sources = voidStats.findSources(null, GEO_LAT.toString(), null, false);
		Assert.assertTrue(sources.size() == 1);
		Assert.assertTrue(15000 == voidStats.getSize(sources.iterator().next()));
		Assert.assertTrue(5000 == (Long) voidStats.pCard(sources.iterator().next(), GEO_LAT.toString()));
		Assert.assertTrue(5000 == (Long) voidStats.typeCard(sources.iterator().next(), GML_FEATURE.toString()));
		
//		sources = voidStats.findGraphs(FOAF_NAME.toString());
		sources = voidStats.findSources(null, FOAF_NAME.toString(), null, false);
		Assert.assertTrue(sources.size() == 1);
		Assert.assertTrue(19000 == voidStats.getSize(sources.iterator().next()));
		Assert.assertTrue(9000 == (Long) voidStats.pCard(sources.iterator().next(), FOAF_NAME.toString()));
		Assert.assertTrue(9000 == (Long) voidStats.typeCard(sources.iterator().next(), FOAF_PERSON.toString()));
	}
	
}
