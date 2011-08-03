# !/bin/sh

#mainclass=de.uni_koblenz.west.statistics.void2.Void2StatisticsGenerator
mainclass=de.uni_koblenz.west.statistics.util.VoidGenerator

# collect all jars in lib/ for inclusion in the classpath
for jar in lib/*.jar; do classpath=$classpath:$jar; done

# add current dir and bin/ dir to classpath
classpath=.:bin/$classpath

# run main class with classpath setting
java -cp $classpath -Xmx2g $mainclass $* 1>void.n3

