# !/bin/sh
# test the source selection for differnt configurations

# collect all jars
for jar in `ls ../../lib/*.jar`; do
  path=$path:$jar
done

# run data generation
for config in `ls ../eval_selection_*.properties`; do
  echo java -cp ../../bin/$path de.uni_koblenz.west.federation.test.eval.SourceSelectionEval $config
#  java -cp ../../bin/$path de.uni_koblenz.west.federation.test.eval.SourceSelectionEval $config
done 

# generate charts from data using gnuplot
cd plots
gnuplot source-selection_number-of-sources.plt \
        source-selection_requests-fragments.plt \
        source-selection_requests-patterns.plt
cd ..

## prevent gs (when called by epstopdf) from eager page rotation
export GS_OPTIONS="-dAutoRotatePages=/All"

# convert ps to pdf
cd charts
for ps in `ls *.ps`; do
#  echo $ps -> ${ps%.*}.pdf
  ps2pdf -dEPSCrop $ps
#  epstopdf $ps # does not work correctly: wront rotation
  pdfcrop --noverbose --margins 10 ${ps%.*}.pdf ${ps%.*}.pdf
done
rm *.ps
cd ..

