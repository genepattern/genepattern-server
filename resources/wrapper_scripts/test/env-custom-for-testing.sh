#!/usr/bin/env bash

#
# for 'env-test.sh' test-cases
#

#
# test-case: alias
#
putValue 'Matlab-2013a-MCR' 'matlab/2013a'

#
# test-case: dependency with aliases
#     R-3.0 is aliased to R/3.0.1
#     R/3.0.1 depends on gcc/4.7.2
putValue 'R-3.0' 'gcc/4.7.2, R/3.0.1'

# 
# test-case: set default java version (e.g. run-with-env -u Java mapped to 'use Java-1.8')
# Note: transitive dependencies must be declared explicitly
# this won't work:
#     putValue 'Java' 'Java-1.8'
#     putValue 'Java-1.8' 'java/1.8.1'
# this will:
putValue 'Java' 'java/1.8.1'
putValue 'Java-1.8' 'java/1.8.1'

