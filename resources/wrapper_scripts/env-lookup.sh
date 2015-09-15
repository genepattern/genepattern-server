#!/usr/bin/env bash

#
# Manage module runtime environment name and site customizations
#

# optionally set envMap to an array
# Each key is a canonical name for a module runtime environment,
# Each value is a site specific value, used to load that environment,
# For example, a DotKit name for a Broad hosted GP servers.
# the values are site-specific customizations

#
# Utility function for getting the directory which contains this script
# Usage: scriptDir
#
function scriptDir() {
    local SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
    echo "$SCRIPT_DIR";
}

source "$(scriptDir)/env-hashmap.sh"

#
# Convert relative path to fully qualified path, if necessary. 
# Usage: initPath (<relativePath> | <fullyQualifiedPath>)
#
function initPath() {
    if [[ "$1" = /* ]]; then
        echo "$1";
    else
        echo "$(scriptDir)/$1"; 
    fi
}

#
# Helper function for deciding where to look for the site specific
# module runtime environment customization script.
# If '$GP_ENV_CUSTOM' is set, use that value.
# Else if '$1' is set, use that value.
# Otherwise, use the default value of './env-custom.sh'
#
function initCustomValuePath() {
    local env_custom_script;
    if ! [ -z ${GP_ENV_CUSTOM+x} ]; then
        echo "$(initPath $GP_ENV_CUSTOM)";
        return;
    elif ! [ -z ${1+x} ]; then
        echo "$(initPath $1)";
        return;
    fi
    echo "$(initPath 'env-custom.sh')";
}

# seed the lookup table with default canonical values 
# technically, this is not necessary, but it is helpful for debugging
# and for guidance for GP server admins
function sourceEnvDefault() {
  source "$(scriptDir)/env-default.sh"
}

function sourceEnvCustom() {
    local env_custom_script=$(initCustomValuePath "$@") 
    if [ -f "${env_custom_script}" ]
    then 
        source "${env_custom_script}"
    fi
}

function initValues() {
  sourceEnvDefault
  sourceEnvCustom
}

#
# parse an arg of the form '-e' '<key>=<value>' and export the given value
# special-case: for '=val, ignore
# special-case: for 'key=', unset key
# special-case: for 'key', set key to the empty string
#
# Usage: exportEnv <key>=[<value>]
#
function exportEnv() {
    IFS='=' read -r -a args <<< "$1"
    local key="${args[0]}";
    local val="${args[1]}";
    if [ "${#key}" -eq 0 ]; then
        # debug: echo "ignoring $1, key.length==0";
        return;
    fi
    
    if [ "${#val}" -eq 0 ]; then
        # special-case: check for '=' sign, which means 'unset'
        if [[ $1 == *=* ]]; then
            # debug: echo "unset $key, val.length==0";
            unset $key;
            return;
        else
            # debug: echo "setting $key to empty value"
            export "$key="
            return;
        fi
    fi
    
    # debug: echo "exporting ... $1"
    export "$1"
}


