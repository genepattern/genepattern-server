#!/usr/bin/env bash

#
# Site specific customizations for wrapper environment for GenePattern Server
# installation at Indiana University
#

function initEnv() {
    module load "$1" &>/dev/null
}

putValue 'Java'             'java/1.7.0_51'
putValue 'Java-1.7'         'java/1.7.0_51'
putValue 'Matlab-2013a-MCR' 'matlab/2013a'
putValue 'Python-2.5'       'python/2.5.4'
putValue 'Python-2.6'       'python/2.6.9'
putValue 'R-2.5'            'R/2.5.1'
putValue 'R-2.7'            'R/2.7.2'
putValue 'R-2.10'           'R/2.10.1'
putValue 'R-2.11'           'R/2.11.1'
putValue 'R-2.13'           'R/2.13.2'
putValue 'R-2.14'           'R/2.14.2'
putValue 'R-2.15'           'R/2.15.2'
putValue 'R-3.0'            'gcc/4.7.2, R/3.0.1'
putValue 'R-3.1'            'gcc/4.7.2, R/3.1.1'
putValue 'GCC-4.7'          'gcc/4.7.2'

