#!/usr/bin/env bash
#
# Wrapper script for Rscript 3.0 installer module
#
# First initialize the environment 
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use .r-3.0.3-vanilla

# Load the OpenGL dependencies required for the 'rgl' package and set it to run headless.
# This is required as they are not present on the LSF9 compute nodes.  We need to do this
# for (at least) Barbara Weir's PARIS_benchmark module.
use .libmesa_from_matlab-2014b
export RGL_USE_NULL=TRUE

# Now, modify the environment variables that affect the R environment
export R_ENVIRON=/xchip/gpdev/servers/genepatterntest/gp/resources/R30_Environ
export R_PROFILE=/xchip/gpdev/servers/genepatterntest/gp/resources/R30_Profile

# Forcibly ignore any user-level environ file and profile.
export R_ENVIRON_USER=
export R_PROFILE_USER=

Rscript "$@"  2>&1
