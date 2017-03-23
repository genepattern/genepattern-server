#!/usr/bin/env bash

############################################################
# env-hashmap.sh
#     bash3 compatible alternative to an associative array
# Usage:
#     source env-hashmap.sh
# Declared variables:
#   __gp_module_envs
#   _hashmap_keys
#   _hashmap_vals
# Declared functions:
#   putValue canonical-name [local-name][,local-name]* 
#   getValue canonical-name
#   addEnv module-name
#   clearValues
#   
#   (helpers)
#   __indexOf() {
#
############################################################

if [[ "${_env_hashmap_inited:-}" -eq 1 ]]; then
    return;
else 
    readonly _env_hashmap_inited=1;
    declare -a _hashmap_keys=();
    declare -a _hashmap_vals=();
    declare -a __gp_module_envs=();
fi

############################################################
# Function: putValue, add site-specific mapping to the (virtual) hashmap
# Usage:
#   putValue canonical-name [site-specific-name]*
# Output: Makes changes to these variables
#   _hashmap_keys
#   _hashmap_vals
# Examples:
# 1) override canonical name with local name,
#     putValue 'matlab-mcr/2010b' '.matlab_2010b_mcr'
# 2) map canonical name to two local names (aka dotkits)
#     putValue 'R/3.1.3' 'gcc/4.7.2, R/3.1'
#
# Note: when value not set, use the key as the value, which 
#   is allows the lookup function to behave as a no-op when
#   no site-specific customization is set
############################################################
putValue() {
    if [[ "$#" -eq 2 ]]; then
        local key="${1}";
        local val="${2}";
    elif [[ "$#" -eq 1 ]]; then
        # special-case, just one arg, use the key as the value
        local key="${1}";
        local val="${1}";
    else 
        echo "Usage: putValue key [value]"
        exit 1;
    fi
    
    # special-case for empty map
    # if isEmpty; then
    if [[ 0 -eq $(numKeys) ]]; then
        _hashmap_keys=( "${key}" );
        _hashmap_vals=( "${val}" );
        return;
    fi

    local idx=$(__indexOf "${key}");
    if [[ "${idx}" = "-1" ]]; then
        _hashmap_keys=( "${_hashmap_keys[@]}" "${key}" )
        _hashmap_vals=( "${_hashmap_vals[@]}" "${val}" )
    else 
        _hashmap_keys[$idx]=$key;
        _hashmap_vals[$idx]=$val;
    fi
}

############################################################
# Function: getValue, get the local value for the given canonical name
# Usage:
#   getValue canonical-name
# Returns:
#   site-specific-name, defaults to canonical-name
# References:
#   __indexOf()
#   _hashmap_vals
############################################################
getValue() {
    local key="${1}"
    local idx=$(__indexOf "${key}");
    if [[ "${idx}" = "-1" ]]; then
        echo "${key}";
        return;
    else 
        local val="${_hashmap_vals[$idx]}";
        echo "${val}";
        return;
    fi
}

############################################################
# Function: addEnv, add an entry to the '__gp_module_envs' array
# Usage:
#   addEnv module-name
# References:
#   __gp_module_envs
############################################################
addEnv() {
    local key="$1";
    local val="$(getValue "${key}")";
    
    oldIFS="$IFS";
    IFS=', '
    read -a valArr <<< "$val"
    IFS="$oldIFS"

    # step through each value
    local idx;
    for idx in "${!valArr[@]}"
    do
        if [[ 0 -eq $(numEnvs) ]]; then 
             __gp_module_envs=( "${valArr[idx]}" )
        else 
            __gp_module_envs=( "${__gp_module_envs[@]}" "${valArr[idx]}" )
        fi
    done
}

############################################################
# Function: __indexOf, utility function, get index of key in map
# Usage:
#   __indexOf key
# References:
#   _hashmap_keys
############################################################
__indexOf() {
    local key="${1}";
    local i=0;
    # if isEmpty; then 
    if [[ 0 -eq $(numKeys) ]]; then
        echo "-1";
        return;
    else
        local str;
        for str in "${_hashmap_keys[@]}"; do
            if [[ "$str" = "$key" ]]; then
                echo "${i}";
                return;
            else
                ((i++))
            fi
        done
    fi
    echo "-1";
}

############################################################
# Function: clearValues, reset the map
# References:
#   _hashmap_keys
#   _hashmap_vals
#   _gp_module_envs
############################################################
clearValues() {
    _hashmap_keys=();
    _hashmap_vals=();
    __gp_module_envs=();
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

__size_of() { echo "$#"; }

numKeys() { 
    echo $(__size_of "${_hashmap_keys[@]:0}");
}

numEnvs() { 
    echo $(__size_of "${__gp_module_envs[@]:0}"); 
}

# return 0, success if there are no keys
# return 1, failure if there are keys
isEmpty() {
    if [[ 0 -eq $(numKeys) ]]; then return 0;
    else return 1;
    fi
}

# return 0, success if there are no runtime environments
# return 1, failure if there are runtime environments
isEmptyEnv() {
    if [[ 0 -eq $(numEnvs) ]]; then return 0;
    else return 1;
    fi
}

echoCounts() {
    echo "num keys: $(numKeys)";
    echo "num envs: $(numEnvs)";
}

echoValues() {
    echo "keys=${_hashmap_keys[@]}"
    echo "_hashmap_vals=${_hashmap_vals[@]}"
}

echoHashmap() {
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
