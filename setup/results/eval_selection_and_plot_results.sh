# !/bin/sh
# test the source selection for differnt configurations

# collect all jars
for jar in `ls ../../lib/*.jar`; do
  path=$path:$jar
done

for config in `ls ../eval_selection_*.properties`; do
  echo java -cp ../../bin/$path de.uni_koblenz.west.federation.test.eval.SourceSelectionEval $config
  java -cp ../../bin/$path de.uni_koblenz.west.federation.test.eval.SourceSelectionEval $config
done 


cd plots
gnuplot source-selection_number-of-sources.plt \
        source-selection_requests-fragments.plt \
        source-selection_requests-patterns.plt
cd ..

