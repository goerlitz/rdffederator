# !/bin/sh
# test the source selection for differnt configurations

# get extension of output file from command line; default=eps
ext=${1:-eps}

# init directories
mypath=`pwd`
subdir=$mypath
datdir=$subdir/results
pltdir=$subdir/plots
outdir=$subdir/charts

# set right output terminal for extension
if [[ "$ext" = "png" ]]
then
  term="png enhanced truecolor size 900,500"
else
  term="postscript eps enhanced solid \"Helvetica,16\""
fi

# generate charts from data using gnuplot via a crafted plot file
for plot in $pltdir/*.plt; do

  # use name of input file for output file but change file extension
  outfile=${plot%.*}.$ext        # replace file extension
  outfile=$outdir/${outfile##*/}   # replace directory name

  # set ouput terminal and output file
  echo "set terminal $term" >$datdir/temp.plt
  echo "set output \"$outfile\"" >>$datdir/temp.plt

  # append gnuplot template and actual plot file to crafted plot file
  cat $pltdir/source-selection.template >>$datdir/temp.plt
  cat $plot >>$datdir/temp.plt
  
  cd $datdir
  gnuplot temp.plt
  echo plotting $outfile
  rm temp.plt
  cd $mypath
done

# convert eps to pdf
if [[ "$ext" = "eps" ]]
then

  ## prevent gs (when called by epstopdf) from eager page rotation
  export GS_OPTIONS="-dAutoRotatePages=/All"
  
  cd $outdir
  for ps in *.eps; do
    ps2pdf -dEPSCrop $ps
    # 'epstopdf $ps' does not work correctly (wrong rotation)
    pdfcrop --noverbose --margins 10 ${ps%.*}.pdf ${ps%.*}.pdf
  done
  rm *.eps
  cd $mypath
fi

