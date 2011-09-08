# !/bin/sh
#
# Generator for DARQ service descriptions.
# Input:  RDF graph in N-Triples format (one triple per line).
# Output: DARQ service description in N3 format.
#####################################################################

# use handy variable names for arguments
ntriples=$1;
statfile=$2;

# check existence of input file
if [ ! -e $ntriples ]; then
    echo "ERROR: RDF input file '$ntriples' does not exist";
    exit;
fi

# check existence of output file, do not overwrite
if [ -e $statfile ]; then
    echo "ERROR: output file '$statfile' already exist";
    # TODO: ask for permission to overwrite
    exit;
fi

start=$(date +%s)

echo "counting triples, properties, and types"
#declare -a map_pred_type

# count number of occurrences of predicates and types in input file
# ignore blank and comment lines (sed is faster than awk)
#map_pred_type=($(cat $1 | awk 'NF && ($1 !~ "^#") {
map_pred_type=($(cat $ntriples | sed '/^ *#/d;/^ *$/d' | awk '{
  s=$1; p=$2; o=$3

  # check URIs (regular splitting fails for URIs with whitespace)
  if ( ! (p ~ /^</ && p ~ />$/) ) {  # slow, but faster than /^<.*>$/
    # do splitting again taking spaces in URIs into account
    col=0;
    s=$++col; while (s ~ /^</ && ! (s ~ />$/)) s=s" "$++col
    p=$++col; while (p ~ /^</ && ! (p ~ />$/)) p=p" "$++col
    o=$++col; for (i=++col;i<=NF-1;i++) o=o" "$i
  }

  # count predicates and types
  pred[p]++
  if (p == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") type[o]++
}
END {
  OFS="\t";
  # print count and URI of predicate/type with prefix 'P:' or 'T:'
  for (no in pred) print pred[no],"P:"no
  for (no in type) print type[no],"T:"no
}' | sort -k2))

# count triples, predicates, and types
let "map_count = ${#map_pred_type[*]} / 2"
for (( i = 0 ;  i < $map_count;  i++ )); do
  value=${map_pred_type[$i*2+1]}
  if [ ${value%%<*} == "P:" ]; then
    let "pred_count++"
    let "triple_count += ${map_pred_type[$i*2]}"
  else
    let "type_count++"
  fi
done

# create DARQ regex for all types
# regex="REGEX(STR(?object),'${types[1]}')"
for (( i = $pred_count ;  i < $map_count;  i++ )); do
  if [ "$regex" != "" ]; then regex=$regex" || "; fi
  type=${map_pred_type[$i*2+1]}
  type=${type#T:<};type=${type%>} # remove prefix and angle brackets
  regex=$regex"REGEX(STR(?object),'$type')"
done

# print header
echo  >$statfile "@prefix sd: <http://darq.sf.net/dose/0.1#> ."
echo >>$statfile ""
echo >>$statfile "[] a sd:Service ;"

# print statistics
echo >>$statfile -e "\tsd:totalTriples \"$triple_count\" ;"
for (( i = 0 ;  i < $pred_count;  i++ )); do
  pred=${map_pred_type[$i*2+1]}
  pred=${pred#P:}; # remove prefix
  echo >>$statfile -e "\t"`[ $i = 0 ] && echo "sd:capability [" || echo "] , ["`
  echo >>$statfile -e "\t\tsd:predicate $pred ;"
  echo >>$statfile -e "\t\tsd:triples \"${map_pred_type[$i*2]}\" ;"
  if [ "$pred" == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" ]; then
    echo >>$statfile -e "\t\tsd:sofilter \"$regex\"^^<http://www.w3.org/2001/XMLSchema#string> ;"
  else
    echo >>$statfile -e "\t\tsd:sofilter \"\" ;"
  fi
done
echo >>$statfile -e "\t] ."

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
