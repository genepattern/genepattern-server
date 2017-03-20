#!/usr/bin/env bash

#
# Wrapper script for initializing the runtime environment before running 
# a GenePattern module on a compute node.
#
# Usage: run-with-env.sh \
#    [-c <env-custom-site.sh>] \ 
#    [-e GP_ENV_CUSTOM=<env-custom-site.sh>] \
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
#     ./run-with-env.sh -c env-custom-macos.sh -u R-3.1 echo "Hello" 
#
#
# The GP_ENV_CUSTOM environment variable can also be used to set an alternate name for the
# customization file.
# E.g.
#     export GP_ENV_CUSTOM="env-custom-macos.sh"; ./run-with-env.sh -u R-3.1 echo "Hello"
#
#
# The optional '-e' flag can set an environment variable as a command line arg.
# E.g.
#     ./run-with-env.sh -e MY_KEY=MY_VALUE echo "Hello" 
#
#     special-case: unset the env variable like this, -e "MY_KEY="
#     special-case: set the env variable as an empty value like this, -e "MY_KEY"
#


function main() {
    local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
    source "${__dir}/gp-common.sh"
    #parseArgs "${@}"
    #"${__gp_module_cmd[@]}"
    run "${@}"
}

main "${@}"
