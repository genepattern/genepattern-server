#!/usr/bin/env bash

#
# Submit an lsf job as a shell script so that we can use the DotKit.
#
# cmd line args: -u <dotkit.0> -u <dotkit.1> ... -u <dotkit.N> <cmds>
# for each -u option, 'use' the dotkit 
# then execute the rest of the command line
#
# Usage:
# Configure the GP server to use the script as the executable.
# Make sure to pass in the list of required dotkits on on the command line.
#

#variable to store the list of dotkits from the command line
declare -a dotkits
idx=0
while getopts u: opt "$@"; do
    #Note: couldn't invoke use command from within this loop
    dotkits[idx]=$OPTARG;
    idx=$((idx+1))
    # need this line to remove the -u <dotkit> from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done

if [ $idx -gt 0 ]
then
    #debug: echo "calling useuse ..."
    . /broad/tools/scripts/useuse
fi

for dotkit in "${dotkits[@]}"
do
  #debug: echo calling reuse $dotkit
  reuse "$dotkit" &>/dev/null
done

#debug echo "running cmd: $@"
"$@"
