#!/usr/bin/env bash

#
# for debugging, print the input args as an array
#

idx=0
for arg in "${@}"
do
    idx=$((idx+1))
    echo "arg[$idx]: '${arg}'"
done


#mycmd=( "${@}" )
#arraylength=${#mycmd[@]}
#for (( i=1; i<${arraylength}; i++ ));
#do
#   echo "arg[$i]: ${mycmd[$i]}"
#done

#debug: 
#echo "running cmd: ${mycmd[@]}"
#"$@"

#"${mycmd[@]}" 
