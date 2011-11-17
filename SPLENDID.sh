# !/bin/sh
#
# USAGE: SPLENDID.sh <config> <query>
#################################################

mainclass=de.uni_koblenz.west.splendid.SPLENDID

# set classpath
classpath=./bin

# include all jar files in classpath
for jar in lib/*.jar; do classpath=$classpath:$jar; done

# run SPLENDID
java -cp $classpath $mainclass $*

