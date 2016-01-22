#!/usr/bin/env bash

#
# site customizations for Broad hosted GP servers running legacy CentOS 5 nodes
#

. /broad/software/scripts/useuse

function initEnv() {
    reuse "$1" &>/dev/null
    
    # special-case for FLAME modules
    if [ "$1" = "R-2.7" ]; then
        export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/xchip/gpdev/shared_libraries"
    fi

}

putValue 'Java' 'Java-1.7'
putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
putValue 'Matlab-2013a-MCR' '.matlab_2013a_mcr'
putValue 'Matlab-2013a' '.matlab-2013a'

