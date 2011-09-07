# !/bin/sh
# generates DARQ service descriptions from ntriple files.
# ATTENTION: absolute URIs containing white spaces are not handled correctly

# check script arguments
if [ $# -lt 2 ]; then
    echo "USAGE: ${0##*/} RDF_FILE.nt DARQ_FILE.n3";
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
echo  >$voidfile "@prefix sd: <http://darq.sf.net/dose/0.1#> ."
echo >>$voidfile ""
echo >>$voidfile "[] a sd:Service ;"

# define arrays for the properties, types, and distinct subjects/objects
declare -a props types dists disto

start=$(date +%s)

echo "counting triples and properties"
props=($(awk '{ arr[$2]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' $ntriples | sort -k2))

echo "counting types and entities"
types=($(grep '#type' $ntriples | awk '{ arr[$3]++ } END { OFS="\t"; for(no in arr) { print arr[no], no } }' | sort -k2))

#echo "counting distinct objects"
#disto=($(./count_predicate_distinct_o $ntriples | sort))

#echo "counting distinct subjects"
#dists=($(./count_predicate_distinct_s $ntriples | sort))

# collect numbers of general statistics
let "p_count = ${#props[*]} / 2"
let "t_count = ${#types[*]} / 2"
#let "s_count = ${dists[$p_count*2+1]}"
#let "o_count = ${disto[$p_count*2+1]}"
for (( i = 0 ;  i < $p_count;  i++ )); do let "triple_count += ${props[$i*2]}"; done

# print general statistics
echo >>$voidfile -e "\tsd:totalTriples \"$triple_count\" ;"

# create DARQ regex for all types
#regex="REGEX(STR(?object),'${types[1]}')"
for (( i = 0 ;  i < $t_count;  i++ )); do
  if [ "$regex" != "" ]; then regex=$regex" || "; fi
  type=${types[$i*2+1]}
  type=${type#<};type=${type%>} # remove angle brackets
  regex=$regex"REGEX(STR(?object),'$type')"
done

#echo "REGEX: $regex"

# print predicate statistics
for (( i = 0 ;  i < $p_count;  i++ )); do
    echo >>$voidfile -e "\t"`[ $i = 0 ] && echo "sd:capability [" || echo "] , ["`
    echo >>$voidfile -e "\t\tsd:predicate ${props[$i*2+1]} ;"
    echo >>$voidfile -e "\t\tsd:triples \"${props[$i*2]}\" ;"
    if [ "${props[$i*2+1]}" == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" ]; then
        echo >>$voidfile -e "\t\tsd:sofilter \"$regex\"^^<http://www.w3.org/2001/XMLSchema#string> ;"
    else
        echo >>$voidfile -e "\t\tsd:sofilter \"\" ;"
    fi
done
echo >>$voidfile -e "\t] ."

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
