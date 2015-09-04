#!/usr/bin/env bash

#
# implement associative array as functions for bash 3 compatibility
#

function _env_hashmap_checkInit() { 
  # initialize new bash3 compatible hashmap
  if [ -z "${_env_hashmap_inited+1}" ]; then    
    declare _env_hashmap_inited="true";
    declare -a _hashmap_keys=();
    declare -a _hashmap_vals=();
    declare -a _runtime_environments=();
  fi
}

# allows this script to be sourced more than once, without blowing away pre-existing keys and vals
_env_hashmap_checkInit;

function indexOf() {
    local key="${1}"
    local i=0;
    for str in "${_hashmap_keys[@]}"; do
        if [ "$str" = "$key" ]; then
            echo "${i}"
            return
        else
            ((i++))
        fi
    done
    echo "-1"
}

function putValue() {
    if [ "$#" -eq 2 ]
    then
        local key="${1}"
        local val="${2}"
    elif [ "$#" -eq 1 ]
    then
        # special-case, just one arg, use the key as the value
        local key="${1}"
        local val="${1}"
    else 
        echo "Usage: putValue <key> [<value>]"
        exit 1
    fi
    local idx=$(indexOf $key);
    if [ $idx = "-1" ]; then
        _hashmap_keys=( "${_hashmap_keys[@]}" "${key}" )
        _hashmap_vals=( "${_hashmap_vals[@]}" "${val}" )
    else 
        _hashmap_keys[$idx]=$key;
        _hashmap_vals[$idx]=$val;
    fi
}

function getValue() {
    local key="${1}"
    local idx=$(indexOf "${key}");
    if [ $idx = "-1" ]; then
        echo "${key}";
        return;
    else 
        local val="${_hashmap_vals[$idx]}";
        echo "${val}";
        return;
    fi
}

function clearValues() {
    _hashmap_keys=();
    _hashmap_vals=();
    _runtime_environments=();
}

function echoValues() {
    echo "keys=${_hashmap_keys[@]}"
    echo "_hashmap_vals=${_hashmap_vals[@]}"
}

#
# module runtime environment specific functions
#
function addEnv() {
    local key="$1";
    local val="$(getValue $key)";
    
    oldIFS="$IFS";
    IFS=', '
    read -a valArr <<< "$val"
    IFS="$oldIFS"

    # step through each value
    for index in "${!valArr[@]}"
    do
        _runtime_environments=( "${_runtime_environments[@]}" "${valArr[index]}" )
    done
}

