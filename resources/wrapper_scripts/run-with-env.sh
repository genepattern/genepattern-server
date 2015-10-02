#!/usr/bin/env bash

#
# Wrapper script for initializing the runtime environment before running 
# a GenePattern module on a compute node.
#
# Usage: run-with-env.sh \
#    [-c <env-custom-site.sh>] \ 
#    -u <dotkit-id.0> -u <dotkit-id.1> ... -u <dotkit-id.N> \  
#    -e <key0=value> ... -e <keyN=value> \
#    <cmd> [<args>]
#
# Each '-u' flag declares a module runtime environment which must be initialized.
# For Broad hosted servers this corresponds to a dotkit name.
#
# Configuration
# Create a new 'env-custom.sh' script as a copy of the 'env-default.sh' file.'
# Modify as needed for your GP instance.
#
# Extra features
# The optional '-c' flag can set an alternate name for the customization file.
# It must be the first arg to the script.
# E.g. 
#     ./run-with-env.sh -c env-custom-broad-centos5.sh -u R-3.1 echo "Hello" 
#
#
# The GP_ENV_CUSTOM environment variable can also be used to set an alternate name for the
# customization file.
# E.g.
#     export GP_ENV_CUSTOM="env-custom-broad-centos5.sh"; ./run-with-env.sh -u R-3.1 echo "Hello"
#
#
# The optional '-e' flag can set an environment variable as a command line arg.
# E.g.
#     ./run-with-env.sh -e MY_KEY=MY_VALUE echo "Hello" 
#
#     special-case: unset the env variable like this, -e "MY_KEY="
#     special-case: set the env variable as an empty value like this, -e "MY_KEY"
#

_gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
# add this directory to the path
export PATH="${_gp_script_dir}:${PATH}"

source "${_gp_script_dir}/env-lookup.sh"
sourceEnvDefault
# special-case: check for -c <gp_env_custom> 
if [ "$1" = "-c" ]; then
    shift;
    sourceEnvCustom "$1"
    shift;
else
    sourceEnvCustom
fi

idx=0
while getopts u:e: opt "$@"; do
    case $opt in
        u)
            addEnv "$OPTARG"
            ;;
        e)
            exportEnv "$OPTARG"
            ;; 
        *)
            # Unexpected option, exit with status of last command
            exit $?
            ;;
    esac
    idx=$((idx+1))
    # need this line to remove the option from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done

for module in "${_runtime_environments[@]}"
do
  initEnv "${module}"
done

# run the command
"${@}" 
