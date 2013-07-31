#!/bin/bash

DATAFILE=$1
TITLE=$2
IMGFILE=$3

gnuplot <<EOF
set terminal postscript noenhanced eps monochrome
set output "${IMGFILE}"
set size 0.5,0.5
set key left top
set rmargin 3

set title "Full callback times for ${TITLE}"
set xlabel "Block size"
set ylabel "Duration (ms)"

set xtics (        \
  "..." 64,      \
  "" 128,          \
  "" 256,          \
  "" 512,          \
  "" 1024,         \
  "2048" 2048,         \
  "4096" 4096,   \
  "8192" 8192 )
set grid

#set yrange [0:70]
set xrange [0:8192]
#set log x 2

set style line 1 lc 1 lt 2 ps 0.7 pt 5

plot "${DATAFILE}" using 2:10 title "${TITLE}" with linespoints ls 1
EOF

epstopdf ${IMGFILE}
rm -rf ${IMGFILE}
