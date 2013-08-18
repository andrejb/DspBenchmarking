#!/bin/bash

PLOT=$1
TITLE=$2
IMGFILE=$3
XMIN=$4
XMAX=$5

gnuplot <<EOF
set terminal postscript enhanced eps color
set output "${IMGFILE}"
set size 1,1
set key below horizontal
set rmargin 3

set title "Valor medio do numero maximo de osciladores na Sintese Aditiva - ${TITLE}"
set ylabel "Numero de osciladores"


set style line 1 lc 1 lt 1
set style line 2 lc 3 lt 1
set style line 3 lc 2 lt 1
set style line 4 lc 4 lt 1

set bmargin 10

set boxwidth 1
set style fill solid 1.0 border 0

set pointsize 0.5

set xtics rotate
set grid

set yrange [0:140]
#set log x 2

set style line 1 lc 1 lt 2 ps 0.7 pt 5

plot ${PLOT}
EOF

epstopdf ${IMGFILE}
rm -rf ${IMGFILE}
