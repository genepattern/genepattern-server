#!/bin/bash

#
# Example site customization script for shunit2 test-cases
#   sourced from 'env-test.sh'
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

# test-case: custom java version for <run-with-env> -u Java ...
putValue 'Java' 'java/1.8'
# test-case: transitive dependency
putValue 'Java-1.8' 'java/1.8'
putValue 'java' 'java/1.8'

# test-case: alias for canonical name='matlab-mcr/2014a'
putValue 'matlab-mcr/2014a' '.matlab_2014a_mcr'

# test-case: alias R-2.5 for <run-rjava> command
putValue 'R-2.5' 'r-2.5-vanilla-gp'

# test-case: set dependency
#   R-3.1 requires GCC-4.9
putValue 'R-3.1' 'GCC-4.9, R-3.1'

# test-case: set dependency with aliases
#   R-3.0 is aliased to R/3.0.1
#   R/3.0.1 depends on gcc/4.7.2
putValue 'R-3.0' 'gcc/4.7.2, R/3.0.1'
putValue 'r/3.0' 'gcc/4.7.2, r/3.0.1'


#   Note: transitive dependencies must be declared explicitly
#   this won't work:
#     putValue 'Java' 'Java-1.8'
#     putValue 'Java-1.8' 'java/1.8.1'
#   this will:
#putValue 'Java-1.7' 'java/1.7'
