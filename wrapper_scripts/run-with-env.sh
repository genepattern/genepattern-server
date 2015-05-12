#!/usr/bin/env bash

#
# Wrapper script for running a GenePattern module on a compute node.
# This script initializes the runtime environment before running the command.
#
# cmd line args: -u <cmd-env-id.0> -u <cmd-env-id.1> ... -u <cmd-env-id.N> <cmd> [<args>]
# for each -u option, initalize the environment 
# then execute the rest of the command line
#
# Usage:
# Configure the GP server to use the script as the executable.
# Pass the list of module environments on on the command line.
#

# to make this script portable, set 'envCmd' in the env-custom.sh file 
source $(dirname $0)/env-init.sh

# initialize the list of environment modules
declare -a modules 
idx=0
while getopts u: opt "$@"; do
    modules[idx]=$(modOrReplace $OPTARG);
    idx=$((idx+1))
    # need this line to remove the -u <module> from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done

for module in "${modules[@]}"
do
  initEnv "${module}"
done

# run the command
"${@}" 
