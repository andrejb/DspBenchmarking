#!/usr/bin/python
# gen-graphics.py
#


import os
import re
import tempfile


RESULTS_DIR = './results'
IMG_DIR = './img'
DEVICE_NAME_KEY = 'device:'
SDK_KEY = 'sdk_int:'
RELEASE_KEY = 'release:'

ALGORITHMS = {
    '0': ("LOOPBACK", "loopback"),
    '1': ("REVERB", "reverb"),
    '2': ("FFT_ALGORITHM", "FFT"),
    '3': ("CONVOLUTION", "convolution"),
    '4': ("ADD_SYNTH_SINE", "add. synth (api sine)"),
    '5': ("ADD_SYNTH_LOOKUP_TABLE_LINEAR", "add. synth (linear lookup)"),
    '6': ("ADD_SYNTH_LOOKUP_TABLE_CUBIC", "add. synth (cubic lookup)"),
    '7': ("ADD_SYNTH_LOOKUP_TABLE_TRUNCATED", "add. synth (truncated lookup)"),
    '8': ("FFTW_MONO", "FFTW (1 thread)"),
    '9': ("FFTW_MULTI", "FFTW (multithread)"),
    '10': ("DOUBLE_FFT", "FFT (double precision)"),
    '11': ("DOUBLE_DCT", "DCT (double precision)"),
    '12': ("DOUBLE_DST", "DST (double precision)"),
    '13': ("DOUBLE_DHT", "DHT (double precision)"),
}

RESULTS = {}


# iterate over result files
for filename in os.listdir(RESULTS_DIR):

    print("Looking into %s..." % filename)

    # ignore .gitignore
    if filename == '.gitignore':
        continue

    # read result file
    f = open('%s/%s' % (RESULTS_DIR, filename), 'r')
    lines = f.readlines()
    f.close()

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
        RESULTS[major] = {}

    #-------------------------------------------------------------------------
    # algorithms
    #-------------------------------------------------------------------------

    # get algorithms
    algs = {}
    algre = re.compile('^#\s+([0-9]+)\s+(.*)$')
    for line in lines:
        m = algre.match(line)
        if m:
            algindex = m.groups(0)[0]
            algname = m.groups(0)[1]
            algs[algindex] = algname
            # create entry for this algorithm in results
            if algindex not in RESULTS[major]:
                RESULTS[major][algindex] = []

    #-------------------------------------------------------------------------
    # create results gnuplot line
    #-------------------------------------------------------------------------

    # create a temporary dir for this result
    tempdir = tempfile.mkdtemp()

    # iterate over algorithms for this result
    for algidx, algname in algs.items():

        print("  looking into %s..." % algname)

        # get results for this algorihtm
        algresults = filter(lambda x: x.startswith("%s " % algidx), lines)
        algresults = map(lambda x: x.strip(), algresults)

        # make sure we just run for algorithms that have results
        if len(algresults) > 0:
        
            # iterate over lines for different block sizes for this algorithm
            colre = re.compile('\s+')
            lineresults = []
            for blockline in algresults:
                info = blockline.split()
                blocksize = info[1]  # block size
                dspcbmeantime = info[9]  # dsp callback mean time
                lineresults.append("%s\t%s" % (blocksize, dspcbmeantime))

            # join results into one well formatted string
            resultstring = '\n'.join(lineresults)

            # store info in temp file
            tmpfile = tempfile.mktemp(dir=tempdir)
            f = open(tmpfile, 'w')
            f.write(resultstring)
            f.flush()
            f.close()
            #print("ARQUIVO %s %s %s" %(algname, devicename, tmpfile))

            # make gnuplot line
            gnuplotline = "'%s' title '%s' with linespoints lw 4" % (tmpfile, devicename)
            #print("    defined gnuplot line: %s" % gnuplotline)
            RESULTS[major][algidx].append(gnuplotline)

# create file with block periods
tmpblockfile = tempfile.mktemp(dir=tempdir)
f = open(tmpblockfile, 'w')
for i in xrange(4,14):
    blocksize = 2**i
    blockperiod = blocksize/44.1
    f.write('%d %f\n' % (blocksize, blockperiod))
f.flush()
f.close()

#print tmpblockfile

# create figures for major versions
for major in RESULTS:
    for algidx, alginfo in ALGORITHMS.iteritems():
        algname, title = alginfo
        print "Generating for major %s algname %s..." % (major, algname)
        # append the DSP period
        RESULTS[major][algidx].append(
            "'%s' title 'DSP period' with lines lt 0 lw 5 lc 0" %
            tmpblockfile)
        # configure the gnuplot script for this algorithm
        imgfile = '%s/%s-%s.eps' % (IMG_DIR, algname, major)
        plotline = ', \\\n'.join(RESULTS[major][algidx])
        scriptline = './algorithm-devices.sh "%s" "%s" %s %d %d %d %d'
        # generate from 16 to 256
        imgfile = '%s/%s-%s-a.eps' % (IMG_DIR, algname, major)
        realtitle = title + ' - API %s (blocos menores)' % major
        os.system(
            scriptline %
            (plotline, title, imgfile, 16, 256, 0, 6))
        # generate from 512 to 8192
        imgfile = '%s/%s-%s-b.eps' % (IMG_DIR, algname, major)
        realtitle = title + ' - API %s (blocos maiores)' % major
        os.system(
            scriptline %
            (plotline, realtitle, imgfile, 512, 8192, 0, 180))
