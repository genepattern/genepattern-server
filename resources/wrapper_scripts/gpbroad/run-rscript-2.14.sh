#!/usr/bin/env bash
#
# Wrapper script for Rscript 2.14
#
# First initialize the environment via the 'use R-2.14' command
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use R-2.14
Rscript "$@"
