#!/usr/bin/env bash

#
# implement associative array as functions for bash 3 compatibility
#

if [[ "${_env_hashmap_inited:-}" -eq 1 ]]; then
    return;
else 
    readonly _env_hashmap_inited=1;
    declare -a _hashmap_keys=();
    declare -a _hashmap_vals=();
    declare -a _runtime_environments=();
fi

# return 0, success if there are no keys
# return 1, failure if there are keys
function isEmpty() {
    if [[ 0 -eq $(numKeys) ]]; then return 0;
    else return 1;
    fi
}

function indexOf() {
    local key="${1}"
    local i=0;
    if isEmpty; then 
        echo "-1"
        return
    else
        local str;
        for str in "${_hashmap_keys[@]}"; do
            if [[ "$str" = "$key" ]]; then
                echo "${i}"
                return
            else
                ((i++))
            fi
        done
    fi
    echo "-1"
}

function putValue() {
    if [[ "$#" -eq 2 ]]; then
        local key="${1}";
        local val="${2}";
    elif [[ "$#" -eq 1 ]]; then
        # special-case, just one arg, use the key as the value
        local key="${1}";
        local val="${1}";
    else 
        echo "Usage: putValue <key> [<value>]"
        exit 1;
    fi
    
    # special-case for empty map
    if isEmpty; then
        _hashmap_keys=( "${key}" );
        _hashmap_vals=( "${val}" );
        return;
    fi

    local idx=$(indexOf $key);
    if [[ "${idx}" = "-1" ]]; then
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
    local idx;
    for idx in "${!valArr[@]}"
    do
        if [[ 0 -eq $(numEnvs) ]]; then 
             _runtime_environments=( "${valArr[idx]}" )
        else 
            _runtime_environments=( "${_runtime_environments[@]}" "${valArr[idx]}" )
        fi
    done
}

#
# helper functions to workaround 'unbound variable' error 
# with empty arrays in 'set -u' mode.
#
# for example, 
#     declare -a _my_arr=();
# adding a value to an empty array causes an ERROR
#     _my_arr=( "${_my_arr[@]}" "new_value" )
# checking the size causes the same error
#     if [[ ${#_my_arr[@]} -eq 0 ]]; 
#
# Instead check the size indirectly ...
# This doesn't work
#     count() { echo $# ; } ; _my_arr=() ; count "${_my_arr[@]}"
# This does work,
#     count() { echo $# ; } ; _my_arr=() ; count "${_my_arr[@]:0}"
#
# See the Bash documentation on the set builtin, and this 
# stack overflow article
#     http://stackoverflow.com/questions/7577052/bash-empty-array-expansion-with-set-u

function __size_of() { echo $#; }

function numKeys() { 
    echo $(__size_of "${_hashmap_keys[@]:0}");
}

function numEnvs() { 
    echo $(__size_of "${_runtime_environments[@]:0}"); 
}

function echoCounts() {
    echo "num keys: $(numKeys)";
    echo "num envs: $(numEnvs)";
}

function echoValues() {
    echo "keys=${_hashmap_keys[@]}"
    echo "_hashmap_vals=${_hashmap_vals[@]}"
}

function echoHashmap() {
    if [[ ${#_hashmap_keys[@]} -eq 0 ]];
    then 
        echo "_hashmap: (no values)"
        return;
    else
        local idx=0;
        local key;
        local val;
        echo "_hashmap ...";
        for key in "${_hashmap_keys[@]}"; do
            val="${_hashmap_vals[$idx]}";
            echo "    $key=$val";
            ((idx++))
        done
    fi
}
