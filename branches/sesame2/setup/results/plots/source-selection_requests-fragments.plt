set yrange [0:25]
set ylabel "# requests"
set title "Number of remote requests to send with sets of triple patterns"

plot 'source-selection_stats_pred.dat' using 3:xtic(1) title "by predicate only" fillstyle pattern 1, \
     'source-selection_stats_pred_rdfType.dat' using 3:xtic(1) title "+ matching RDF type", \
     'source-selection_stats_pred_rdfType_sameAs.dat' using 3:xtic(1) title "+ merge sameAs triples"
