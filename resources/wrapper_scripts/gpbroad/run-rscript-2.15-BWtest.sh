#!/usr/bin/env bash
#
# Wrapper script for Rscript 2.15
#
# First initialize the environment via the 'use R-2.15' command
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use .r-2.15.3-vanilla

# Load the OpenGL dependencies required for the 'rgl' package and set it to run headless.
# This is required as they are not present on the LSF9 compute nodes.  We need to do this
# for (at least) Barbara Weir's PARIS_benchmark module.
use .libmesa_from_matlab-2014b
export RGL_USE_NULL=TRUE

Rscript "$@"
