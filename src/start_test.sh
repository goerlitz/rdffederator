# !/bin/sh

mainclass=de.uni_koblenz.west.federation.test.FederationRepositoryTest

# collect all jars
for jar in ../lib/*.jar; do
  path=$path:$jar
done

echo java -cp .$path $mainclass $*
java -cp .$path $mainclass $*

