#!/usr/bin/env bash
#
# Wrapper script for Java 1.8
#
# First initialize the environment via the 'use Java-1.8' command
# then run Rscript passing along all the command line args
#
. /broad/tools/scripts/useuse
use Java-1.8
java "$@"
