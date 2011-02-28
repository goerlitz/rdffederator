set yrange [0:22]
set ylabel "#requests"
set title "Number of all requests to send with single query patterns"

plot 'source-selection_baseline.dat' using 4:xtic(1) title "predicate-based" fillstyle pattern 1, \
     'source-selection_with-type.dat' using 4:xtic(1) title "rdf:type-based", \
     'source-selection_with-type-and-sameAs.dat' using 4:xtic(1) title "owl:sameAs merge"

