# !/bin/sh
# test the source selection for differnt configurations

mainclass=de.uni_koblenz.west.evaluation.SourceSelectionEval

# collect all jars
for jar in ../lib/*.jar; do
  path=$path:$jar
done

for config in source-selection_*; do
  echo java -cp ../bin$path $mainclass $config
  java -cp ../bin$path $mainclass $config
done 

