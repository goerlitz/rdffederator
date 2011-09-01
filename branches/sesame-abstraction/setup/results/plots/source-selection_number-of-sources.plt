set yrange [0:12]
set ylabel "#sources"
set title "Number of selected data sources"

plot 'source-selection_baseline.dat' using 2:xtic(1) title "based on predicate" fillstyle pattern 1, \
     'source-selection_with-type.dat' using 2:xtic(1) title "+ consider object type", \
     'source-selection_with-type-and-sameAs.dat' using 2:xtic(1) title "+ merge sameAs triples"

