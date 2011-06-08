set yrange [0:25]
set ylabel "# requests"
set title "Number of remote requests to send with single triple patterns"

plot 'source-selection_baseline.dat' using 4:xtic(1) title "by predicate only" fillstyle pattern 1, \
     'source-selection_with-type.dat' using 4:xtic(1) title "+ matching RDF type", \
     'source-selection_with-type-and-sameAs.dat' using 4:xtic(1) title "+ merge sameAs triples"

