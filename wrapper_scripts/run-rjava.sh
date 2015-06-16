#!/usr/bin/env bash
#
# Wrapper script for running R as a java command
#
# arg1: the version number of R to run, e.g. '2.15'
#

r_version=$1
shift;

script_dir=$(dirname $0)
source $(dirname $0)/env-init.sh

initEnv $(modOrReplace "R-${r_version}")
initEnv $(modOrReplace "Java")

r=`which R`
rhome=${r%/*/*}
java -DR_HOME=$rhome -Dr_flags='--no-save --quiet --slave --no-restore' "$@"

