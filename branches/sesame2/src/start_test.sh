# !/bin/sh

mainclass=de.uni_koblenz.west.federation.test.FederationRepositoryTest

# collect all jars in lib path
for jar in lib/*.jar; do
  classpath=$classpath:$jar
done

classpath=.:bin/:$classpath

# run main class with all jars and bin/ dir in classpath
echo java -cp $classpath $mainclass $*
java -cp $classpath $mainclass $*

