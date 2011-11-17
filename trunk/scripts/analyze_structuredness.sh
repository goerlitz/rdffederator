# !/bin/sh

mainclass=de.uni_koblenz.west.splendid.statistics.util.StructurednessAnalyzer

# add current dir and bin/ to the classpath
classpath=.:bin/

# add all jars in lib/ to the classpath
for jar in lib/*.jar; do classpath=$classpath:$jar; done

# run main class with classpath setting
java -cp $classpath -Xmx2g $mainclass $*

