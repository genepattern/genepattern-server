#!/bin/bash

# strip off any arguments meant for the old run-with-env.sh script since
# any environment stuff should be st up by/within the container

while getopts u:e: opt "$@"; do
    case $opt in
        u)
            #addEnv "$OPTARG"
            ;;
        e)
            #exportEnv "$OPTARG"
            ;; 
        *)
            # Unexpected option, exit with status of last command
            #exit $?
            ;;
    esac
    idx=$((idx+1))
    # need this line to remove the option from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done


"$@"  



