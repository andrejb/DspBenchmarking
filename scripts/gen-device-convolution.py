# gen-graphics.py
#


import os
import re
import tempfile
import numpy


RESULTS_DIR = './results'
IMG_DIR = './img'
DEVICE_NAME_KEY = 'device:'
SDK_KEY = 'sdk_int:'
RELEASE_KEY = 'release:'


ALGORITHMS = {
    '3': ("CONVOLUTION", "convolucao"),
}


RESULTS = {}

# iterate over result files
for filename in os.listdir(RESULTS_DIR):

    # ignore .gitignore
    if filename == '.gitignore':
        continue

    print("Looking into %s..." % filename)

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


    #-------------------------------------------------------------------------
    # info about device
    #-------------------------------------------------------------------------

    # get device name
    namere = re.compile("^#\s+%s\s+(.+)$" % DEVICE_NAME_KEY)
    devicename = ''
    for line in lines:
        m = namere.match(line)
        if m:
            devicename = m.groups(0)[0]
    print("  device: %s" % devicename)

    # get android release
    namere = re.compile("^#\s+%s\s+(.+)$" % RELEASE_KEY)
    release = ''
    for line in lines:
        m = namere.match(line)
        if m:
            release = m.groups(0)[0]
    print("  release: %s" % release)

    # get major version
    versionre = re.compile('([0-9])\.[0-9](\.[0-9])?')
    major = versionre.match(release).groups(0)[0]
    print("  major version: %s" % major)

    # store major in results
    if major not in RESULTS:
        RESULTS[major] = []

    #-------------------------------------------------------------------------
    # algorithms
    #-------------------------------------------------------------------------

    # get algorithms
    algs = {}
    for algindex, alginfo in ALGORITHMS.items():
        algkey, algname = alginfo
        algs[algindex] = algname
        # create entry for this algorithm in results
        #if algindex not in RESULTS[major]:
        #    RESULTS[major][algindex] = []
    
    # create a temporary dir for this result
    tempdir = tempfile.mkdtemp()

    # iterate over algorithms for this result
    means = {}
    for algidx, algname in algs.items():
        # get results for this algorihtm
        algresults = filter(lambda x: x.startswith("%s " % algidx), lines)
        algresults = map(lambda x: x.strip(), algresults)

        # iterate over lines for different block sizes for this algorithm
        colre = re.compile('\s+')
        results = []
        for blockline in algresults:
            results.append(colre.split(blockline))

        # get mean of stress param for all blocks
        stress = []
        for blockresult in results:
            stress.append(float(blockresult[11]))  # stress param
        #print "%s %s (%s):" % (filename, algname, algidx)
        #print stress
        stddev = numpy.std(stress)
        mean = numpy.mean(stress)
        #print "mean: %f" % mean
        #print "stddev: %f" % stddev
        # remove os dados que estao abaixo de 2 desvios padroes
        stress = filter(lambda x: x > mean - stddev, stress)
        #print stress
        #print "new mean: %f" % (sum(stress)/len(stress))
        means[algidx] = sum(stress)/len(stress)

    # api sine, truncated, linear, cubic
    realmeans = (devicename, means['3'])
    RESULTS[major].append('%s %f' % realmeans)


# plot results
for major in RESULTS:

    # join results into one well formatted string
    resultstring = '\n'.join(RESULTS[major])

    # store info in temp file
    tmpfile = tempfile.mktemp(dir=tempdir)
    f = open(tmpfile, 'w')
    f.write(resultstring)
    f.flush()

    plotlines = []
    title = 'convolucao'
    plotlines.append("'%s' using 2: xtic(1) title '%s' with histogram ls 1" %
        (tmpfile, title))
    plot = ', '.join(plotlines)

    # run the gnuplot script for this algorithm
    imgfile = '%s/CONVOLUTION_COMPARISON-%s.eps' % (IMG_DIR, major)
    os.system(
        './device-convolution.sh "%s" "%s" %s %d %d' % (plot, 'API %s' % major,
        imgfile, 0, 140))
