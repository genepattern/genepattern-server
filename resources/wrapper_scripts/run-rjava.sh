#!/usr/bin/env bash

############################################################
# run-rjava.sh
#   Java wrapper for legacy R modules, which pre-date the 
# Rscript command.
# Usage:
#   run-rjava.sh -c env-custom r-version java-flags -cp classpath RunR args
# Example module commandLine
#   commandLine=<R2.5> <libdir>hello.R hello
#   commandLine=<R2.15_Rjava> <libdir>hello.R hello
# Configuration
#   run-rjava=<wrapper-scripts>/run-rjava.sh -c <env-custom>
#   R2.15_Rjava=<run-rjava> 2.15 <rjava_flags> -cp <run_r_path> RunR
############################################################

function run_rjava() {
    local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
    source "${__dir}/gp-common.sh"
    
    # optional '-c <env-custom>' flag
    if [[ "${1:-}" = "-c" ]]; then
        shift; 
        # only set if '-c' has an argument
        if ! [[ -z "${1+x}" || $1 = -* ]]; then
            __gp_env_custom_arg="${1:-}";
            shift;
        fi
    fi
    
    local r_version="${1}"
    shift;

    # the remaining args are the module command line
    __gp_module_cmd=( "$@" );

    # process '-e' flags immediately
    export_envs;

    # process '-c' flag next
    source_env_scripts;
    
    # process '-u' flags after site-customization
    add_module_envs;

    # customization for run-rjava.sh script
    addEnv "R-${r_version}"
    addEnv "Java"
    init_module_envs;

    r=`which R`
    rhome=${r%/*/*}
    java "-DR_HOME=${rhome}" -Dr_flags='--no-save --quiet --slave --no-restore' "$@"
}

run_rjava "${@}"
