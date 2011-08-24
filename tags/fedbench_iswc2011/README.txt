========================================
 SPLENDID - A SPARQL Endpoint Federator 
========================================

= OVERVIEW =

SPLENDID provides transparent SPARQL query federation for distributed RDF
data sources. A query is split up into fragments which are sent to selected
SPARQL endpoints which are expected to return results for the query expression.
Join order optimization is done based on a Dynamic Programming approach using
statistical information from voiD description to estimate the result size
cardinality.

= CONFIGURATION =

SPLENDID is based on the Sesame 2 sail architecture. A SPLENDID federation can
be set up via Sesame's standard repository configuration mechanism.

Example setup files can be found in setup/sail-config/.

The repository configuration needs federation members with voiD descriptions
containing statistical information. A generator for such void statistics
is located in de.uni_koblenz.west.statistics.util.VoidStatisticsGenerator.
