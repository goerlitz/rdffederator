# !/bin/sh

# collect all jars
for jar in `ls ../lib/*.jar`; do
  path=$path:$jar
done

echo java -cp .$path de.uni_koblenz.west.federation.test.FederationRepositoryTest $*
java -cp .$path de.uni_koblenz.west.federation.test.FederationRepositoryTest $*

