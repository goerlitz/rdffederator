# !/bin/sh
# generates void statistics from ntriple files.

# check script arguments
# if [ $# = 0 ]; then
if test $# -lt 2; then
    echo "USAGE: ${0##*/} RDF_FILE.nt VOID_FILE.n3";
    exit;
fi

ntriples=$1;
voidfile=$2;

# check existence of input file
if [ ! -e $ntriples ]; then
    echo "ERROR: RDF input file '$ntriples' does not exist";
    exit;
fi

# check existence of output file, do not overwrite
if [ -e $voidfile ]; then
    echo "ERROR: VOID output file '$voidfile' already exist";
    # TODO: ask for permission to overwrite
    exit;
fi

# generate void output
echo  >$voidfile "@prefix void: <http://rdfs.org/ns/void#> ."
echo >>$voidfile ""
echo >>$voidfile "[] a void:Dataset ;"

# define arrays for the properties, types, and distinct subjects/objects
declare -a props types dists disto

start=$(date +%s)

echo "counting triples and properties"
props=($(awk '{ arr[$2]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' $ntriples | sort -k2))

echo "counting types and entities"
types=($(grep '#type' $ntriples | awk '{ arr[$3]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' | sort -k2))

echo "counting distinct objects"
disto=($(./count_predicate_distinct_o $ntriples | sort))

echo "counting distinct subjects"
dists=($(./count_predicate_distinct_s $ntriples | sort))

# collect numbers of general statistics
let "p_count = ${#props[*]} / 2"
let "t_count = ${#types[*]} / 2"
let "s_count = ${dists[$p_count*2+1]}"
let "o_count = ${disto[$p_count*2+1]}"
for (( i = 0 ;  i < $p_count;  i++ )); do let "triple_count += ${props[$i*2]}"; done

# print general statistics
echo >>$voidfile -e "\tvoid:triples \"$triple_count\" ;"
echo >>$voidfile -e "\tvoid:classes \"$t_count\" ;"
echo >>$voidfile -e "\tvoid:properties \"$p_count\" ;"
echo >>$voidfile -e "\tvoid:distinctSubjects \"$s_count\" ;"
echo >>$voidfile -e "\tvoid:distinctObjects \"$o_count\" ;"

# print predicate statistics
for (( i = 0 ;  i < $p_count;  i++ )); do
    echo >>$voidfile -e "\t"`[ $i = 0 ] && echo "void:propertyPartition [" || echo "] , ["`
    echo >>$voidfile -e "\t\tvoid:property ${props[$i*2+1]} ;"
    echo >>$voidfile -e "\t\tvoid:triples \"${props[$i*2]}\" ;"
    echo >>$voidfile -e "\t\tvoid:distinctSubjects \"${dists[$i*2+1]}\" ;"
    echo >>$voidfile -e "\t\tvoid:distinctObjects \"${disto[$i*2+1]}\" ;"
done
echo >>$voidfile -e "\t] ;"

# print type statistics
for (( i = 0 ;  i < $t_count;  i++ )); do
    echo >>$voidfile -e "\t"`[ $i = 0 ] && echo "void:classPartition [" || echo "] , ["`
    echo >>$voidfile -e "\t\tvoid:class ${types[$i*2+1]} ;"
    echo >>$voidfile -e "\t\tvoid:entities \"${types[$i*2]}\" ;"
done
echo >>$voidfile -e "\t] ."

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
