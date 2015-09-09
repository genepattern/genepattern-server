#!/usr/bin/env bash

#
# Wrapper script for <java> command line module.
# 
# Alternate implementations:
# 1) Skip this script and use a substitution,
#    java=<run-with-env> -u Java java
#
# 2) directly call run-with-env.sh
#    "${script_dir}/run-with-env.sh" -u Java java "${@}" 

script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "$script_dir/env-lookup.sh"
sourceEnvDefault
# special-case: check for -c <gp_env_custom> 
if [ "$1" = "-c" ]; then
    shift;
    sourceEnvCustom "$1"
    shift;
else
    sourceEnvCustom
fi

addEnv "Java"
for module in "${_runtime_environments[@]}"
do
  initEnv "${module}"
done

java "${@}"
