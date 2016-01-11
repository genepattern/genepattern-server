#!/usr/bin/env bash

#
# Site specific customizations for wrapper environment for GenePattern Server
# installation at Indiana University
#

function initEnv() {
    # extract root package name, e.g. "R" from "R/2.15.3"
    package="${1%%\/*}";
    module unload "$package" &>/dev/null
    module load "$1" &>/dev/null

    # check exit code and fail if there are any problems
    # Note: as an improvement I'd like to capture output to a log file 
    #     rather than swallow to /dev/null
    if [ $? -ne 0 ]; then
        >&2 echo "Error loading $1, contact the server admin";
        exit $1
    fi
}

putValue 'Java'             'java/1.7.0_51'
putValue 'Java-1.7'         'java/1.7.0_51'
putValue 'Matlab-2013a-MCR' 'matlab/2013a'
putValue 'Python-2.5'       'python/2.5.4'
putValue 'Python-2.6'       'python/2.6.9'
putValue 'Python-2.7'       'python/2.7.3'
putValue 'R-2.5'            'R/2.5.1'
putValue 'R-2.7'            'R/2.7.2'
putValue 'R-2.10'           'R/2.10.1'
putValue 'R-2.11'           'R/2.11.1'
putValue 'R-2.13'           'R/2.13.2'
putValue 'R-2.14'           'R/2.14.2'
putValue 'R-2.15'           'R/2.15.3'
# 
# Note from Le-Shin:
# the default gcc version on mason is 4.4.7, and we need gcc > 4.7 to install and run R/3.0 and R/3.1.
#
putValue 'R-3.0'            'gcc/4.7.2, R/3.0.1'
putValue 'R-3.1'            'gcc/4.7.2, R/3.1.1'
putValue 'GCC-4.7'          'gcc/4.7.2'


