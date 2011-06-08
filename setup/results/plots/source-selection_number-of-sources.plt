set yrange [0:12]
set ylabel "# selected sources"
set title "Number of selected data sources per query"

plot 'source-selection_baseline.dat' using 2:xtic(1) title "by predicate only" fillstyle pattern 1, \
     'source-selection_with-type.dat' using 2:xtic(1) title "+ matching RDF type", \
     'source-selection_with-type-and-sameAs.dat' using 2:xtic(1) title "+ merge sameAs triples"

