#!/usr/bin/env bash

#
# declare default implementation for initializing a runtime environment
# Usage: initEnv <runtime-env-name>
#
function initEnv() {
    if is_debug; then
        # only when GP_DEBUG="true"
        echo "initEnv(): initializing '$1' environment ..."
    fi
}

#
# initialize the default, canonical runtime environment names
#

putValue 'java' 'Java-1.7'
putValue 'Java' 'Java-1.7'
putValue 'Java-1.7'
putValue 'Matlab-2010b-MCR'
putValue 'Matlab-2013a-MCR'
putValue 'Perl' 'Perl-5.10'
putValue 'Perl-5.10'
putValue 'Python-2.5'
putValue 'Python-2.6'
putValue 'Python-2.7'
putValue 'R-2.0'
putValue 'R-2.5'
putValue 'R-2.7'
putValue 'R-2.10'
putValue 'R-2.11'
putValue 'R-2.13'
putValue 'R-2.14'
putValue 'R-2.15'
putValue 'R-3.0'
putValue 'R-3.1'
putValue 'GCC-4.9'
