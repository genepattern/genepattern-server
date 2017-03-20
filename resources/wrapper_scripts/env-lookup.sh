#!/usr/bin/env bash

# include this script once and only once
[[ "${_env_lookup_inited:-}" -eq 1 ]] && return || readonly _env_lookup_inited=1

#
# Manage module runtime environment name and site customizations
#

# optionally set envMap to an array
# Each key is a canonical name for a module runtime environment,
# Each value is a site specific value, used to load that environment,
# For example, a DotKit name for a Broad hosted GP servers.
# the values are site-specific customizations




##
## Helper function for deciding where to look for the site specific
## module runtime environment customization script.
## If '$GP_ENV_CUSTOM' is set, use that value.
## Else if '$1' is set, use that value.
## Otherwise, use the default value of './env-custom.sh'
##
#function initCustomValuePath() {
#    local env_custom_script;
#    if ! [ -z ${GP_ENV_CUSTOM+x} ]; then
#        echo "$(convertPath $GP_ENV_CUSTOM)";
#        return;
#    elif ! [ -z ${1+x} ]; then
#        echo "$(convertPath $1)";
#        return;
#    fi
#    echo "$(convertPath 'env-custom.sh')";
#}



##
## parse an arg of the form '-e' '<key>=<value>' and export the given value
## special-case: for '=val, ignore
## special-case: for 'key=', unset key
## special-case: for 'key', set key to the empty string
##
## Usage: exportEnv <key>=[<value>]
##
#function exportEnv() {
#    IFS='=' read -r -a args <<< "$1"
#    local key="${args[0]}";
#    local val="";
#    if [[ ${#args[@]} > 1 ]]; then 
#        val="${args[1]}";
#    fi
#
#    if [ "${#key}" -eq 0 ]; then
#        # debug: echo "ignoring $1, key.length==0";
#        return;
#    fi
#    
#    if [ "${#val}" -eq 0 ]; then
#        # special-case: check for '=' sign, which means 'unset'
#        if [[ $1 == *=* ]]; then
#            # debug: echo "unset $key, val.length==0";
#            unset $key;
#            return;
#        else
#            # debug: echo "setting $key to empty value"
#            export "$key="
#            return;
#        fi
#    fi
#    
#    # debug: echo "exporting ... $1"
#    export "$1"
#}

##
## Append an element to the end of the path; 
## Usage: path=$(appendPath "${path}" "${element}")
##
#function appendPath() {
#    local path="${1}";
#    local element="${2}";
#    
#    # Note, to check for a directory: [ -d "$element" ] 
#    # To prepend, path="$element:$path"
#    
#    # if path is not set ... just set it to element
#    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
#    if [ -z "$path" ]; then
#        #echo "2, path not set";
#        path="$element";
#    elif [[ ":$path:" != *":$element:"* ]]; then
#        path="${path:+"$path:"}$element"
#    fi
#    # use echo to return a value
#    echo "$path"
#}

##
## Prepend an element to the beginning of the path; 
## Usage: path=$(prependPath "${element}" "${path}")
##
#function prependPath() {
#    local element="${1}";
#    local path="${2}";
#    
#    # Note, to check for a directory: [ -d "$element" ] 
#    # To prepend, path="$element:$path"
##    
#    # if path is not set ... just set it to element
#    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
#    if [[ -z "$path" ]]; then
#        #echo "2, path not set";
#        path="$element";
#    elif [[ ":$path:" != *":$element:"* ]]; then
#        path="$element:$path";
#    fi
#    # use echo to return a value
#    echo "$path"
#}

