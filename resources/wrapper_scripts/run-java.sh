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

function run_java() {
    local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
    source "${__dir}/gp-common.sh"
    parse_args "${@}";
    addEnv "Java";
    init_module_envs;
    java "${__gp_module_cmd[@]}"
}

run_java "${@}"
