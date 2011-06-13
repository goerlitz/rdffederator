set yrange [0:12]
set ylabel "# selected sources"
set title "Number of selected data sources per query"

plot 'source-selection_stats_pred.dat' using 2:xtic(1) title "stats, predicate only" fillstyle pattern 1, \
     'source-selection_stats_pred_rdfType.dat' using 2:xtic(1) title "stats, with RDF type", \
     'source-selection_stats_pred_rdfType_sameAs.dat' using 2:xtic(1) title "stats, with RDF type/ sameAs", \
     'source-selection_ask.dat' using 2:xtic(1) title "SAPRQL ASK", \
     'source-selection_ask_sameAs.dat' using 2:xtic(1) title "SPARQL ASK with sameAs"

