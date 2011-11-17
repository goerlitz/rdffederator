# !/bin/sh
#
# run SPLENDID federation test

# set default config file if not specified
cfg=${1:-"federation-test.properties"}

mainclass=de.uni_koblenz.west.federation.test.FederationRepositoryTest

# set classpath
classpath=../bin

# include all jar files in classpath
for jar in ../lib/*.jar; do classpath=$classpath:$jar; done

#logoption="-Dlog4j.configuration=file:../src/log4j.properties"

# run federation test
java -cp $classpath $mainclass $cfg

