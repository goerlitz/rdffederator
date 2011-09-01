# !/bin/sh
# test the source selection for differnt configurations

# get extension of output file from command line; default=eps
ext=${1:-eps}

if [[ "$ext" = "png" ]]
then
  term="png enhanced truecolor size 900,500"
else
  term="postscript eps enhanced solid \"Helvetica\" 14"
fi

# generate charts from data using gnuplot via a crafted plot file
for plot in `ls plots/*.plt`; do
  # use name of input file for output file but change file extension
  outfile=${plot%.*}.$ext        # replace file extension
  outfile=charts/${outfile#*/}   # replace directory name
  cat <<EOF >temp.plt
set terminal $term
set output "$outfile"
EOF

  # append gnuplot template and actual plot file to crafted plot file
  cat plots/source-selection.template >>temp.plt
  cat $plot >>temp.plt
  gnuplot temp.plt
  echo plotting $outfile
done
rm temp.plt


# convert eps to pdf
if [[ "$ext" = "eps" ]]
then

  ## prevent gs (when called by epstopdf) from eager page rotation
  export GS_OPTIONS="-dAutoRotatePages=/All"
  
  cd charts
  for ps in `ls *.eps`; do
    ps2pdf -dEPSCrop $ps
#    epstopdf $ps # does not work correctly: wront rotation
    pdfcrop --noverbose --margins 10 ${ps%.*}.pdf ${ps%.*}.pdf
  done
  rm *.eps
  cd ..
fi

