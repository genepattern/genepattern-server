#!/usr/bin/env bash

#
# Wrapper script for Java cmd
#

declare -a cmd;
cmd=( "$(dirname $0)/run-with-env.sh" 
    "-u" "Java-7" 
    "java" "$@" )

"${cmd[@]}"

