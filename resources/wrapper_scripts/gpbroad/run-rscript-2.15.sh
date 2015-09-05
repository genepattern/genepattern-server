#!/usr/bin/env bash
#
# Wrapper script for Rscript 2.15
#
# First initialize the environment via the 'use R-2.15' command
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use R-2.15

Rscript "$@"
