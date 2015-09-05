#!/usr/bin/env bash
#
# Wrapper script for Rscript 3.1
#
# First initialize the environment via the 'use R-3.1' command
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use R-3.1
Rscript "$@"
