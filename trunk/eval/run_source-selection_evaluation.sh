# !/bin/sh
#
# run source selection tests using different configurations

mainclass=de.uni_koblenz.west.evaluation.SourceSelectionEval

# set classpath
classpath=../bin

# include all jar files in classpath
for jar in ../lib/*.jar; do classpath=$classpath:$jar; done

# create result directory if it does not exist
[ -d results ] || (mkdir results && echo "created result directory")

# execute tests for all configuration files in current folder
for config in source-selection_*.properties; do
  echo java -cp $classpath $mainclass $config $*
  java -cp $classpath $mainclass $config $*
done 

