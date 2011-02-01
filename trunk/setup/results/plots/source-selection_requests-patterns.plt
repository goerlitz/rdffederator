#set terminal png size 1000, 550
set terminal png truecolor size 600,350
set size ratio 0.5
set output "../charts/source-selection_requests-patterns.png"

set title "Number of all requests to send with single query patterns"

set ylabel "#requests"
set yrange [0:20]

#set xtics
#set xtics  norangelimit
#set xtics border in scale 1,0.5 nomirror rotate by -45  offset character 0, 0, 0

#set xtics scale 0 0

set boxwidth 0.8 relative

set auto x
set style data histograms
#set style histogram
set style histogram cluster gap 1

#set style fill solid border -1
#set style fill solid 1.0 border -1
set style fill pattern border

plot '../source-selection_baseline.dat' using 4:xtic(1) title "predicate-based" fillstyle pattern 1, \
     '../source-selection_rdf-type.dat' using 4:xtic(1) title "rdf:type-based", \
     '../source-selection_same-as.dat' using 4:xtic(1) title "owl:sameAs merge"

