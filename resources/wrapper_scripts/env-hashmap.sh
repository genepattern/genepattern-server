#!/usr/bin/env bash

[[ "${_env_hashmap_inited:-}" -eq 1 ]] && return || readonly _env_hashmap_inited=1

#
# implement associative array as functions for bash 3 compatibility
#

declare -a _hashmap_keys=();
declare -a _hashmap_vals=();
declare -a _runtime_environments=();

function indexOf() {
    local key="${1}"
    local i=0;
    if [[ ${#_hashmap_keys[@]} -eq 0 ]];
    then 
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
export -f indexOf;
readonly -f indexOf;

#function indexOfParameterized() {
#    local key_table="${1}_keys";
#    local key="${2}"
#    local i=0;
#    #if [[ ${#_hashmap_keys[@]} -eq 0 ]];
#    if [[ ${#!key_table[@]} -eq 0 ]];
#        then 
#        echo "-1"
#        return
#    else
#        local str;
#        for str in "${!key_table[@]}"; do
#            if [[ "$str" = "$key" ]]; then
#                echo "${i}"
#                return
#            else
#                ((i++))
#            fi
#        done
#    fi
#    echo "-1"
#}

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
    
    # special-case for empty map
    if [[ ${#_hashmap_keys[@]} -eq 0 ]];
    then
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
export -f putValue;
readonly -f putValue;

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
export -f getValue;
readonly -f getValue;

#function getParameterizedValue() {
#    local map="${1}";
#    local key="${2}"
#    local idx=$(indexOf "${key}");
#    if [ $idx = "-1" ]; then
#        echo "${key}";
#        return;
#    else 
#        local val="${_hashmap_vals[$idx]}";
#        echo "${val}";
#        return;
#    fi
#}
#export -f getValue;
#readonly -f getValue;

function clearValues() {
    _hashmap_keys=();
    _hashmap_vals=();
    _runtime_environments=();
}
export -f clearValues;
readonly -f clearValues;

#function _clearValues() {
#  local _arg="_hashmap";
#  local keys="${arg}_keys";
#  local vals="${arg}_vals";
#  
#}
#    local arg="_hashmap";
#    local keys="${arg}_keys";
#    local vals="${arg}_vals";
#
#    ${!keys}=();
#    ${!vals}=();
#    
#    _hashmap_keys=();
#    _hashmap_vals=();
#    _runtime_environments=();
#}


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
        #_runtime_environments=( "${_runtime_environments[@]}" "${valArr[idx]}" )
        if [[ ${#_runtime_environments[@]} -eq 0 ]];
        then
            _runtime_environments=( "${valArr[idx]}" )
        else
            _runtime_environments=( "${_runtime_environments[@]}" "${valArr[idx]}" )
        fi
    done
}
export -f addEnv;
readonly -f addEnv;

function echoValues() {
    echo "keys=${_hashmap_keys[@]}"
    echo "_hashmap_vals=${_hashmap_vals[@]}"
}
export -f echoValues;
readonly -f echoValues;

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
export -f echoHashmap;
readonly -f echoHashmap;
