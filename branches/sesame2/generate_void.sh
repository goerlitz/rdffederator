# !/bin/sh
# generates void descriptions from ntriple files.

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

# check existence of output file: will not overwrite
if [ -e $voidfile ]; then
    echo "ERROR: VOID output file '$voidfile' already exist";
    # TODO: ask for permission to overwrite
    exit;
fi

# generate void output
echo >>$voidfile "@prefix void: <http://rdfs.org/ns/void#> ."
echo >>$voidfile ""
echo >>$voidfile "[] a void:Dataset ;"

# define arrays for the properties and types
declare -a props
declare -a types

echo "counting triples and properties"
props=($(awk '{ arr[$2]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' $ntriples | sort -k2))
let "count = ${#props[*]} / 2"
for ((  i = 0 ;  i < $count;  i++  )); do
    if [ $i = 0 ]; then
        echo >>$voidfile -e "\tvoid:propertyPartition ["
    else
        echo >>$voidfile -e "\t] , ["
    fi
    echo >>$voidfile -e "\t\tvoid:property ${props[$i*2+1]} ;"
    echo >>$voidfile -e "\t\tvoid:triples \"${props[$i*2]}\" ;"
    let "triple_count += ${props[$i*2]}"
done
echo -e >>$voidfile "\t] ;"


echo "counting entities and types"
types=($(grep '#type' $ntriples | awk '{ arr[$3]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' | sort -k2))
let "count = ${#types[*]} / 2"
for ((  i = 0 ;  i < $count;  i++  )); do
    if [ $i = 0 ]; then
        echo >>$voidfile -e "\tvoid:classPartition ["
    else
        echo >>$voidfile -e "\t] , ["
    fi
    echo -e >>$voidfile "\t\tvoid:class ${types[$i*2+1]} ;"
    echo -e >>$voidfile "\t\tvoid:entities \"${types[$i*2]}\" ;"
    let "class_count += ${types[$i*2]}"
done
echo -e >>$voidfile "\t] ;"


echo "counting distinct subjects"
distinct_sub=$(awk '{ arr[$3]++ } END { for(no in arr) { print no } }' $ntriples | wc -l)
echo "counting distinct objects"
distinct_obj=$(awk '{ ORS=""; line=""; for (i=3;i<=NF-1;i++) line=line $i" "; arr[line"\n"]++} END { for(no in arr) { print no } }' $ntriples | wc -l)


# print general statistics
echo -e >>$voidfile "\tvoid:triples \"$triple_count\" ;"
echo -e >>$voidfile "\tvoid:properties \"$((${#props[*]} / 2))\" ;"
echo -e >>$voidfile "\tvoid:classes \"$((${#types[*]} / 2))\" ;"
echo -e >>$voidfile "\tvoid:entities \"$class_count\" ;"
echo -e >>$voidfile "\tvoid:distinctSubjects \"$distinct_sub\" ;"
echo -e >>$voidfile "\tvoid:distinctObjects \"$distinct_obj\" ."

