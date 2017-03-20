#!/bin/bash

#
# Example site specific customizations to be called from shunit2 test
#

function initEnv() {
    # special-case for .libmesa_from_matlab-2014b 
    if [ "$1" = ".libmesa_from_matlab-2014b" ]; then
        export RGL_USE_NULL=TRUE
    fi
    
    # exact copy of env-default.sh
    if ! [ -z ${GP_DEBUG+x} ]; then
        # only when the GP_DEBUG flag is set
        echo "loading $1 ..."
    fi
}
#export -f initEnv

putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
putValue 'Matlab-2013a-MCR' '.matlab_2013a_mcr'
putValue 'R-3.1' 'R-3.1, GCC-4.9'
putValue 'Java' 'custom/java'
putValue 'R-2.5' 'custom/R/2.5'

