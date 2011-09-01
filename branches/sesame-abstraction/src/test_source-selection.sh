# !/bin/sh
# test the source selection for differnt configurations

# collect all jars
for jar in `ls ../lib/*.jar`; do
  path=$path:$jar
done

for config in `ls ../setup/source_selection_*`; do
  echo java -cp .$path de.uni_koblenz.west.federation.test.eval.SourceSelectionEval $config
  java -cp .$path de.uni_koblenz.west.federation.test.eval.SourceSelectionEval $config
done 

