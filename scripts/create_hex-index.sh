# !/bin/sh
#
# experimental script to create indexes for all s,p,o combinations.

# check script arguments
if test $# -lt 1; then
    echo "USAGE: ${0##*/} N-TRIPLES";
    exit;
fi

ntriples=$1;

# check existence of input file
if [ ! -e $ntriples ]; then
    echo "ERROR: RDF input file '$ntriples' does not exist";
    exit;
fi

start=$(date +%s)

# predicates
cat $ntriples | awk '
{
  p=$2; # predicate

  # build id lookup table for predicates
  if (!(p in p_list))
    p_list[p] = count++;
}
END {
  count=0;

  # process all stored subjects/objects
  for (p in p_list) {
    print p_list[p], p;
  }
}' | sort -n >$ntriples.pid

# subjects
cat $ntriples | awk '
{
  s=$1; # predicate

  # build id lookup table for predicates
  if (!(s in s_list))
    s_list[s] = count++;
}
END {
  count=0;

  # process all stored subjects/objects
  for (s in s_list) {
    print s_list[s], s;
  }
}' | sort -n >$ntriples.sid

# objects
cat $ntriples | awk '
{
  FS="[ ]";
  o=$3;
  for (i=4;i<=NF-1;i++)
    o=o" "$i;

  # build id lookup table for predicates
  if (!(o in o_list))
    o_list[o] = count++;
}
END {
  count=0;

  # process all stored subjects/objects
  for (o in o_list) {
    print o_list[o], o;
  }
}' | sort -n >$ntriples.oid

# P:O index
cat $ntriples | awk '
{
  p=$2;
  FS="[ ]";
  o=$3;
  for (i=4;i<=NF-1;i++)
    o=o" "$i;

  # build id lookup table for predicates
  if (!(p in p_list))
    p_list[p] = p_count++;

  # build id lookup table for objects
  if (!(o in o_list))
    o_list[o] = o_count++;

  po_list[p_list[p] ":" o_list[o]]++;
}
END {
  count=0;

  # process all stored subjects/objects
  for (i in po_list) {
    print i, po_list[i];
  }
}' | sort -n >$ntriples.poidx

# S:P index
cat $ntriples | awk '
{
  p=$2;
  s=$1;

  # build id lookup table for predicates
  if (!(p in p_list))
    p_list[p] = p_count++;

  # build id lookup table for subjects
  if (!(s in s_list))
    s_list[s] = s_count++;

  sp_list[p_list[p] ":" s_list[s]]++;
}
END {
  count=0;

  # process all stored subjects/objects
  for (i in sp_list) {
    print i, sp_list[i];
  }
}' | sort -n >$ntriples.spidx

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
