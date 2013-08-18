#!/bin/bash

PLOT=$1
TITLE=$2
IMGFILE=$3
XMIN=$4
XMAX=$5
YMIN=$6
YMAX=$7

gnuplot <<EOF
set terminal postscript enhanced eps color
set output "${IMGFILE}"
set size 0.8,1
set key below horizontal
set rmargin 3

set title "Tempo de execucao da rotina DSP - Algoritmo ${TITLE}"
set xlabel "Block size"
set ylabel "Duration (ms)"

set pointsize 0.5

set xtics (    \
  "16" 16,     \
  "32" 32,     \
  "64" 64,     \
  "128" 128,   \
  "256" 256,   \
  "512" 512,   \
  "1024" 1024, \
  "2048" 2048, \
  "4096" 4096, \
  "8192" 8192 )
set grid

set yrange [${YMIN}:${YMAX}]
set xrange [${XMIN}:${XMAX}]
#set log x 2

set style line 1 lc 1 lt 2 ps 0.7 pt 5

plot ${PLOT}
EOF

epstopdf ${IMGFILE}
rm -rf ${IMGFILE}
