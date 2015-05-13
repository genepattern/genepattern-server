#!/usr/bin/env bash
#
# Wrapper script for running R as a java command
# The first arg ($1) must be the name of the 'module environment' to load, e.g.
#
#    run-rjava.sh 'R-2.7' ...
#

source $(dirname $0)/env-init.sh

initEnv $(modOrReplace "$1")
initEnv $(modOrReplace "Java")

r=`which R`
rhome=${r%/*/*}
java -DR_HOME=$rhome -Dr_flags='--no-save --quiet --slave --no-restore' "$@"

