#!/usr/bin/env bash

#
# site customizations for Broad hosted GP servers running legacy CentOS 5 nodes
#

. /broad/software/scripts/useuse

function initEnv() {
    use "$1" &>/dev/null
}

putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
putValue 'Matlab-2013a-MCR' '.matlab_2013a_mcr'
putValue 'Matlab-2013a' '.matlab-2013a'

