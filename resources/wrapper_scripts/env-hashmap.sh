#!/usr/bin/env bash

############################################################
# env-hashmap.sh
#     bash3 compatible alternative to an associative array
# Usage:
#     source env-hashmap.sh
# Declared variables:
#   __gp_module_envs
#   __gp_env_map_keys
#   __gp_env_map_vals
# Declared functions:
#   putValue canonical-name [local-name][,local-name]* 
#   getValue canonical-name
#   addEnv module-name
#   clearValues
#   
#   (helpers)
#   __indexOf
#   echo_env_map
############################################################

if [[ "${_env_hashmap_inited:-}" -eq 1 ]]; then
    return;
else 
    readonly _env_hashmap_inited=1;
    declare -a __gp_env_map_keys=();
    declare -a __gp_env_map_vals=();
    declare -a __gp_module_envs=();
fi

############################################################
# Function: putValue, add site-specific mapping to the (virtual) hashmap
# Usage:
#   putValue canonical-name [site-specific-name]*
# Output: Makes changes to these variables
#   __gp_env_map_keys
#   __gp_env_map_vals
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
        __gp_env_map_keys=( "${key}" );
        __gp_env_map_vals=( "${val}" );
        return;
    fi

    local idx=$(__indexOf "${key}");
    if [[ "${idx}" = "-1" ]]; then
        __gp_env_map_keys=( "${__gp_env_map_keys[@]}" "${key}" )
        __gp_env_map_vals=( "${__gp_env_map_vals[@]}" "${val}" )
    else 
        __gp_env_map_keys[$idx]=$key;
        __gp_env_map_vals[$idx]=$val;
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
#   __gp_env_map_vals
############################################################
getValue() {
    local key="${1}"
    local idx=$(__indexOf "${key}");
    if [[ "${idx}" = "-1" ]]; then
        echo "${key}";
        return;
    else 
        local val="${__gp_env_map_vals[$idx]}";
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
#   __gp_env_map_keys
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
        for str in "${__gp_env_map_keys[@]}"; do
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
#   __gp_env_map_keys
#   __gp_env_map_vals
#   _gp_module_envs
############################################################
clearValues() {
    __gp_env_map_keys=();
    __gp_env_map_vals=();
    __gp_module_envs=();
}

############################################################
# Functions: 
#   numKeys, count the number of __gp_env_map_keys
#   numEnvs, count the number of __gp_module_envs
#   __num_args (helper), the number of positional parameters
#
# This is a workaround for the 'unbound variable' error when
# in 'set -u' mode and the variable is not set or 
# is an empty array
#
# Background: this example code fails
#   set -u
#   declare -a _my_arr=();
#   # case 1: adding an element to the array causes error
#   _my_arr=( "${_my_arr[@]}" "new_value" )
#   # case 2: checking the size causes the same error
#     if [[ ${#_my_arr[@]} -eq 0 ]]; 
# As a workaround, count the number of elements in the array
# using echo and a function call, use parameter expansion as a 
# guard for unset variables.
#   # this doesn't work
#   count() { echo $# ; } ; _my_arr=() ; count "${_my_arr[@]}"
#   # This does work
#   count() { echo $# ; } ; _my_arr=() ; count "${_my_arr[@]:0}"
############################################################
numKeys() { 
    echo $(__num_args "${__gp_env_map_keys[@]:0}");
}

numEnvs() { 
    echo $(__num_args "${__gp_module_envs[@]:0}"); 
}
__num_args() { echo $#; }

############################################################
# Function: echo_env_map, for debugging, 
#   print the __gp_env_map keys and vals
############################################################
echo_env_map() {
    local pad="${1:-    }";
    if [[ -z ${__gp_env_map_keys+x} ]]; then
        echo "${pad}__gp_env_map:  (no items)";
        return;
    else
        local idx=0;
        local key;
        local val;
        echo "${pad}__gp_env_map:  (${#__gp_env_map_keys[@]} items)";
        for key in "${__gp_env_map_keys[@]}"; do
            val="${__gp_env_map_vals[$idx]}";
            echo "${pad}    $key=$val";
            ((idx++))
        done
    fi
}
