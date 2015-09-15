#!/usr/bin/env bash
#
# Wrapper script for Python 2.7
#
# First initialize the environment via the 'use Python-2.7' command
# then run python passing along all the command line args
#
. /broad/tools/scripts/useuse
reuse Python-2.7
python "$@"
