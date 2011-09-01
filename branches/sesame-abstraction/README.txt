RDF Federator 

OVERVIEW
--------

RDF Federator allows for distributed SPARQL over remote RDF data sources

The implementation is based on the Sesame 3 framework. A Federation Sail
executes a supplied SPARQL query transparently across suitable data sources.


CONFIGURATION
-------------

The federation can be set up via Sesame's standard configuration mechanism
(see package federation.test)

The configuration file need federation members with voiD 2 descriptions
containing statistical information. A generator for such void statistics
is located in de.uni_koblenz.west.statistics.util.VoidStatisticsGenerator.



DATA SOURCE SELECTION
---------------------

Data sources are transparently selected based on available void 2 statistics.

ATTENTION: currently all data sources will be selected for querying.


QUERY OPTIMIZATION
------------------

The query optimization is based on cardinality and selectivity estimates.
Therefore statistical information about the data sources must be available.
Statistics must be provided as a voiD 2 description. 


OPEN ISSUES
-----------

* Optimization of queries which do not contain only basic graph pattern
* Execution of queries with joins over blank nodes