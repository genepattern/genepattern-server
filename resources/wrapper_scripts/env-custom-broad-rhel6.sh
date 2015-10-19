#!/usr/bin/env bash

#
# site customizations for Broad hosted GP servers submitting jobs
# to the Univa Grid Engine queue (UGER) with RHEL6 nodes
#
# see: https://it.broadinstitute.org/wiki/UGER
#

function initEnv() {
    # must source useuse each time
    . /broad/software/scripts/useuse
    use "$1" &>/dev/null

    # special-case for .libmesa_from_matlab-2014b
    if [ "$1" = ".libmesa_from_matlab-2014b" ]; then
        export RGL_USE_NULL=TRUE
    fi
}

putValue 'Java' 'Java-1.7'
putValue 'GCC' 'GCC-4.9'
putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
putValue 'Matlab-2013a-MCR' '.matlab_2013a_mcr'
putValue 'Matlab-2013a' '.matlab-2013a'
putValue 'Python-2.5' 'Python-2.6'   ### <---- no Python 2.5 dotkit
putValue 'R-2.0'  '.genepattern-internal-2.0.1'
putValue 'R-2.5'  '.genepattern-internal-2.5.1'
putValue 'R-2.7'  '.genepattern-internal-2.7.2'
putValue 'R-2.11' '.genepattern-internal-2.11.0'
putValue 'R-3.0' 'GCC-4.9, .libmesa_from_matlab-2014b, R-3.0'
