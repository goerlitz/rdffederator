# !/bin/sh

mainclass=de.uni_koblenz.west.federation.test.FederationRepositoryTest

# collect all jars in lib/ for inclusion in the classpath
for jar in lib/*.jar; do classpath=$classpath:$jar; done

#logoption="-Dlog4j.configuration=file:config/log4j.properties"

# add current dir and bin/ dir to classpath
classpath=.:bin/$classpath

# run main class with classpath setting
java -cp $classpath $mainclass $*

