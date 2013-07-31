# gen-graphics.py
#


import os
import re
import tempfile


RESULTS_DIR = './results'
IMG_DIR = './img'
DEVICE_NAME_IDX = 'incremental:'


# iterate over result files
for filename in os.listdir(RESULTS_DIR):

    # read result file
    f = open('%s/%s' % (RESULTS_DIR, filename), 'r')
    lines = f.readlines()

    # get algorithms
    algs = {}
    algre = re.compile('^#\s+([0-9]+)\s+(.*)$')
    for line in lines:
        m = algre.match(line)
        if m:
            algs[m.groups(0)[0]] = m.groups(0)[1]

    # get device name
    namere = re.compile("^#\s+%s\s+(.+)$" % DEVICE_NAME_IDX)
    devicename = ''
    for line in lines:
        m = namere.match(line)
        if m:
            devicename = m.groups(0)[0]

    # create a temporary dir for this result
    tempdir = tempfile.mkdtemp()

    # iterate over algorithms for this result
    for algidx, algname in algs.items():
        # get results for this algorihtm
        algresults = filter(lambda x: x.startswith("%s " % algidx), lines)
        algresults = map(lambda x: x.strip(), algresults)

        # make sure we just run for algorithms that have results
        if len(algresults) > 0:
        
            # iterate over lines for different block sizes for this algorithm
            colre = re.compile('\s+')
            lineresults = []
            for blockline in algresults:
                lineresults.append('\t'.join(colre.split(blockline)))

            # join results into one well formatted string
            resultstring = '\n'.join(lineresults)

            # store info in temp file
            tmpfile = tempfile.mktemp(dir=tempdir)
            f = open(tmpfile, 'w')
            f.write(resultstring)
            f.flush()

            # run the gnuplot script for this algorithm
            imgfile = '%s/%s-%s.eps' % (IMG_DIR, devicename, algname)
            os.system(
                './device-algorithm.sh %s "%s" %s' % (tmpfile, algname, imgfile))
