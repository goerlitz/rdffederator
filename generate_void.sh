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
echo  >$voidfile "@prefix void: <http://rdfs.org/ns/void#> ."
echo >>$voidfile ""
echo >>$voidfile "[] a void:Dataset ;"

# define arrays for the properties and types
declare -a props
declare -a types
declare -a distp

start=$(date +%s)

echo "counting triples and properties"
props=($(awk '{ arr[$2]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' $ntriples | sort -k2))
let "count = ${#props[*]} / 2"
for (( i = 0 ;  i < $count;  i++ )); do
    echo >>$voidfile -e "\t"`[ $i = 0 ] && echo "void:propertyPartition [" || echo "] , ["`
    echo >>$voidfile -e "\t\tvoid:property ${props[$i*2+1]} ;"
    echo >>$voidfile -e "\t\tvoid:triples \"${props[$i*2]}\" ;"

    distp=($(grep ${props[$i*2+1]} $ntriples | awk '{ FS="[ ]"; s_arr[$1]++ ; line=""; for (i=3;i<=NF-1;i++) line=line $i" "; o_arr[line]++ } END { OFS="\t"; for(no in s_arr) { s_count++ } for(no in o_arr) { o_count++ } print s_count, o_count }'))
    echo >>$voidfile -e "\t\tvoid:distinctSubjects \"${distp[0]}\" ;"
    echo >>$voidfile -e "\t\tvoid:distinctObjects \"${distp[1]}\" ;"
    let "triple_count += ${props[$i*2]}"
done
echo >>$voidfile -e "\t] ;"

echo "counting entities and types"
types=($(grep '#type' $ntriples | awk '{ arr[$3]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' | sort -k2))
let "count = ${#types[*]} / 2"
for ((  i = 0 ;  i < $count;  i++  )); do
    echo >>$voidfile -e "\t"`[ $i = 0 ] && echo "void:classPartition [" || echo "] , ["`
    echo >>$voidfile -e "\t\tvoid:class ${types[$i*2+1]} ;"
    echo >>$voidfile -e "\t\tvoid:entities \"${types[$i*2]}\" ;"
    let "class_count += ${types[$i*2]}"
done
echo >>$voidfile -e "\t] ;"


echo "counting distinct subjects"
distinct_sub=$(awk '{ arr[$1]++ } END { for(no in arr) { print no } }' $ntriples | wc -l)
echo "counting distinct objects" # field separator is single space: FS="[ ]"
distinct_obj=$(awk '{ FS="[ ]"; line=""; for (i=3;i<=NF-1;i++) line=line $i" "; arr[line]++} END { for(no in arr) { print no } }' $ntriples | wc -l)

# print general statistics
echo >>$voidfile -e "\tvoid:triples \"$triple_count\" ;"
echo >>$voidfile -e "\tvoid:properties \"$((${#props[*]} / 2))\" ;"
echo >>$voidfile -e "\tvoid:classes \"$((${#types[*]} / 2))\" ;"
echo >>$voidfile -e "\tvoid:entities \"$class_count\" ;"
echo >>$voidfile -e "\tvoid:distinctSubjects \"$distinct_sub\" ;"
echo >>$voidfile -e "\tvoid:distinctObjects \"$distinct_obj\" ."

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
