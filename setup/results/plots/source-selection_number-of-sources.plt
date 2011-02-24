set terminal png enhanced truecolor size 900,500 font "/usr/share/fonts/truetype/ttf-dejavu/DejaVuSerif.ttf"
set terminal postscript
#set terminal postscript enhanced monochrome size 600,320
set size ratio 0.5

set output "../charts/source-selection_number-of-sources.png"
set output "../charts/source-selection_number-of-sources.ps"

set title "Number of sources selected for sending requests"
set key box # linestyle 1
set key top right

set ylabel "#sources"
set yrange [0:12]

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

plot '../source-selection_baseline.dat' using 2:xtic(1) title "predicate-based" fillstyle pattern 1, \
     '../source-selection_with-type.dat' using 2:xtic(1) title "rdf:type-based", \
     '../source-selection_with-type-and-sameAs.dat' using 2:xtic(1) title "owl:sameAs merge"

