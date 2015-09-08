#!/usr/bin/env bash
#
# Wrapper script for running R as a java command
#
# arg1: the version number of R to run, e.g. '2.15'
#

r_version=$1
shift;

script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${script_dir}/env-lookup.sh"
initValues

addEnv "R-${r_version}"
addEnv "Java"
for module in "${_runtime_environments[@]}"
do
  initEnv "${module}"
done

r=`which R`
rhome=${r%/*/*}
java -DR_HOME=$rhome -Dr_flags='--no-save --quiet --slave --no-restore' "$@"

