#set terminal png size 1000, 550
#set terminal png enhanced truecolor size 900,500 font "/usr/share/fonts/truetype/ttf-dejavu/DejaVuSerif.ttf"
set terminal postscript
set size ratio 0.5
#set output "../charts/source-selection_requests-framents.png"
set output "../charts/source-selection_requests-framents.ps"

set title "Number of all requests to send with query fragments"
set key box
#set key top right

set ylabel "#requests"
set yrange [0:25]

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

plot '../source-selection_baseline.dat' using 3:xtic(1) title "predicate-based" fillstyle pattern 1, \
     '../source-selection_with-type.dat' using 3:xtic(1) title "rdf:type-based", \
     '../source-selection_with-type-and-sameAs.dat' using 3:xtic(1) title "owl:sameAs merge"

