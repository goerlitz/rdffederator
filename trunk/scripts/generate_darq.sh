# !/bin/sh
#
# Generator for DARQ service descriptions.
# Input:  RDF graph in N-Triples format (one triple per line).
# Output: DARQ service description in N3 format.
#####################################################################

declare -a map_pred_type

#############
# FUNCTIONS #
#############

# count number of occurrences of predicates and types in input file
function count_pred_type {
map_pred_type=($(awk 'NF && !/^#/ {
  col=0;

  # split subject, predicate, and object (take care of spaces in URIs)
  s=$++col; while (s ~ /^</ && ! (s ~ />$/)) s=s" "$++col
  p=$++col; while (p ~ /^</ && ! (p ~ />$/)) p=p" "$++col
  o=$++col; for (i=++col;i<=NF-1;i++) o=o" "$i

  # count predicates and types
  pred[p]++
  if (p == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") type[o]++
}
END {
  OFS="\t";
  # print count and URI of predicate/type with prefix 'P:' or 'T:'
  for (no in pred) print pred[no],"P:"no
  for (no in type) print type[no],"T:"no
}' $1 | sort -k2))
}

# prints the contents of the supplied map
# see also http://www.unix.com/302186432-post4.html
function print_map {
    # Setting the shell's Internal Field Separator to null
    OLD_IFS=$IFS
    IFS=''

    # Create a string containing "colors[*]"
    local map_string="$1[*]"

    # assign map using indirect variable reference
    local map=(${!map_string})

    # Resetting IFS to default
    IFS=$OLD_IFS

    let "count = ${#map[*]} / 2"
    for (( i = 0 ;  i < $count;  i++ )); do
       echo "${map[$i*2+1]} -> ${map[$i*2]}"
    done
}

########
# MAIN #
########

# use handy variable names for arguments
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

# save stat time
start=$(date +%s)

echo "counting triples, properties, and types"
count_pred_type $ntriples

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
#regex="REGEX(STR(?object),'${types[1]}')"
for (( i = $pred_count ;  i < $map_count;  i++ )); do
  if [ "$regex" != "" ]; then regex=$regex" || "; fi
  type=${map_pred_type[$i*2+1]}
  type=${type#T:<};type=${type%>} # remove angle brackets
  regex=$regex"REGEX(STR(?object),'$type')"
done

# generate void output
echo  >$voidfile "@prefix sd: <http://darq.sf.net/dose/0.1#> ."
echo >>$voidfile ""
echo >>$voidfile "[] a sd:Service ;"

# print general statistics
echo >>$voidfile -e "\tsd:totalTriples \"$triple_count\" ;"

# print predicate statistics
for (( i = 0 ;  i < $pred_count;  i++ )); do
  pred=${map_pred_type[$i*2+1]}
  pred=${pred#P:}; # remove prefix
  echo >>$voidfile -e "\t"`[ $i = 0 ] && echo "sd:capability [" || echo "] , ["`
  echo >>$voidfile -e "\t\tsd:predicate $pred ;"
  echo >>$voidfile -e "\t\tsd:triples \"${map_pred_type[$i*2]}\" ;"
  if [ "$pred" == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" ]; then
    echo >>$voidfile -e "\t\tsd:sofilter \"$regex\"^^<http://www.w3.org/2001/XMLSchema#string> ;"
  else
    echo >>$voidfile -e "\t\tsd:sofilter \"\" ;"
  fi
done
echo >>$voidfile -e "\t] ."

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
