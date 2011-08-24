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

# define arrays for the properties, types, and distinct subjects/objects
#declare -a props types dists disto

start=$(date +%s)

cat $ntriples | ./print_p-IDs | sort -n >$ntriples.pid
cat $ntriples | ./print_s-IDs | sort -n >$ntriples.sid
cat $ntriples | ./print_o-IDs | sort -n >$ntriples.oid
cat $ntriples | ./print_po-index | sort -n >$ntriples.poidx
cat $ntriples | ./print_sp-index | sort -n >$ntriples.spidx

let "time=$(date +%s)-$start"
echo "time taken: $time seconds"
