#!/usr/bin/env bash
#
# Wrapper script for Rscript 3.0
#
# First initialize the environment via the 'use R-3.0' command
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use GCC-4.9
use R-3.0
Rscript "$@"
