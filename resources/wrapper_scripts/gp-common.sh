#!/usr/bin/env bash

#
# gp-common.sh
#
#    A library of bash function declarations for GenePattern wrapper scripts
#
# Usage:
#     source gp-common.sh
#     parseArgs "${@}"
#     "${__gp_module_cmd[@]}"
#
# Note: see more details in the 'run-with-env.sh' comments.
#
# Global variables
#   __gp_script_dir - the path to this directory, all relative paths
#           are relative to this directory 
#   __gp_env_custom_script - the path to the site customization file
#

# include this script once and only once
if [[ "${__gp_inited:-}" -eq 1 ]]; then 
    return;
else
    readonly __gp_inited=1;
    readonly __gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
    export GP_SCRIPT_DIR=${__gp_script_dir};
    
    readonly __gp_script_file="${__gp_script_dir}/$(basename "${BASH_SOURCE[0]}")";
    readonly __gp_script_base="$(basename ${__gp_script_file} .sh)";
    
    source "${__gp_script_dir}/env-hashmap.sh"
fi

declare __gp_env_custom_arg="";
declare __gp_env_custom_script="";
# placeholder for '-e' args
declare -a __gp_e_args=();
# placeholder for '-u' args
declare -a __gp_u_args=();
# the module command line, stripped of configuration args such as '-c', '-e', and '-u'
declare -a __gp_module_cmd=();

# Exit on error. Append || true if you expect an error.
#set -o errexit
# Exit on error inside any functions or subshells.
#set -o errtrace
# Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
#set -o nounset
# Catch the error in case mysqldump fails (but gzip succeeds) in `mysqldump |gzip`
#set -o pipefail
# Turn on traces, useful while debugging but commented out by default
#set -o xtrace

# Get the fully qualified path to the directory which
# includes this soruce file
#
# usage:
#     local _my_dir=$(__init_dir "${BASH_SOURCE[0]}")
#
function __init_dir() {
    # note: the readlink -f command does not work on Mac OS X
    #     ---> don't do this
    #     local __dir=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
    local __dir=$( cd "$( dirname "${1}" )" && pwd );
    echo $__dir;
}

#
# Get the fully qualified path to the wrapper_scripts directory.
# Usage: scriptDir
#
# Note: the readlink -f command does not work on Mac OS X
#     ---> don't do this
#     local SCRIPT_DIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
#
function scriptDir() {
    echo $(__init_dir "${BASH_SOURCE[0]}" );
    #echo "${__gp_script_dir}";
}
readonly -f scriptDir;

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
    local val="";
    if [[ ${#args[@]} > 1 ]]; then 
        val="${args[1]}";
    fi

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

#
# Convert relative path to fully qualified path, if necessary. 
# Usage: convertPath (<relativePath> | <fullyQualifiedPath>)
#
function convertPath() {
    local arg1=${1:-};
    if [[ "${arg1}" = /* ]]; then
        echo "$arg1";
    else
        echo "${__gp_script_dir}/$arg1"; 
    fi
}

#
# Append an element to the end of the path; 
# Usage: path=$(appendPath "${path}" "${element}")
#
function appendPath() {
    local path="${1}";
    local element="${2}";
    
    # Note, to check for a directory: [ -d "$element" ] 
    # To prepend, path="$element:$path"
    
    # if path is not set ... just set it to element
    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
    if [ -z "$path" ]; then
        #echo "2, path not set";
        path="$element";
    elif [[ ":$path:" != *":$element:"* ]]; then
        path="${path:+"$path:"}$element"
    fi
    # use echo to return a value
    echo "$path"
}

#
# Prepend an element to the beginning of the path; 
# Usage: path=$(prependPath "${element}" "${path}")
#
function prependPath() {
    local element="${1}";
    local path="${2}";
    
    # Note, to check for a directory: [ -d "$element" ] 
    # To prepend, path="$element:$path"
    
    # if path is not set ... just set it to element
    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
    if [[ -z "$path" ]]; then
        #echo "2, path not set";
        path="$element";
    elif [[ ":$path:" != *":$element:"* ]]; then
        path="$element:$path";
    fi
    # use echo to return a value
    echo "$path"
}

#
# Get the root name from the given moduleName.
# 
# Usage: extractRootName <moduleName>
#
# Example,
#    echo extractRootName "R/2.15.3"
#    > "R"
#
function extractRootName() {
    rootName="${1%%\/*}";
    echo "$rootName";
}

# load 'env-default.sh' script  
# seed the lookup table with default canonical values 
# technically, this is not necessary, but it is helpful for debugging
# and for guidance for GP server admins
#function sourceEnvDefault() {
#    source "${__gp_script_dir}/env-default.sh"
#}
#export -f sourceEnvDefault;

## optionally load 'env-custom'
#function sourceEnvCustom() {
#    #__gp_env_custom_script=$(envCustomScript "${__gp_env_custom_arg}");
#    if [ -f "${__gp_env_custom_script}" ];
#    then 
#        source "${__gp_env_custom_script}";
#    fi
#}
#export -f sourceEnvCustom;

function debugEnv() {
    echo               "GP_DEBUG: ${GP_DEBUG}";
    echo          "GP_ENV_CUSTOM: ${GP_ENV_CUSTOM}";
    echo    "__gp_env_custom_arg: ${__gp_env_custom_arg}";
    echo "__gp_env_custom_script: ${__gp_env_custom_script}";
    echoHashmap
    echo  "_runtime_environments: ${_runtime_environments[@]}"
    echo        "__gp_module_cmd: ${__gp_module_cmd[@]}"
}

function run() {
    parseArgs "${@}";
    initModuleEnvs;
    "${__gp_module_cmd[@]}";
}

#
# Parse run-with-env command line args
#     input: ${@}, the command line to the script
#     output: an updated command line array, with run-with-env params stripped.
#
# Usage:
#     local module_cmd=$(parseArgs ${@});
#
# Examples,
#
#     # set 'env-custom' as '-c' arg, must be first arg 
#     parseArgs '-c' 'env-custom-macos.sh' ...
#
#     # set 'env-custom' as '-e' environment arg
#     parseArgs -e GP_ENV_CUSTOM=env-custom-macos.sh ...
#
#     # set 'env-custom' as a environment variable
#     GP_ENV_CUSTOM=env-custom-macos.sh ; parseArgs ...
#
function parseArgs() {
    __gp_env_custom_arg="";
    __gp_env_custom_script=;
    __gp_e_args=(); 
    __gp_u_args=();
    __gp_module_cmd=();

    # optional -c <__gp_env_custom_arg>
    if [ "${1:-}" = "-c" ]; then
        shift;
        __gp_env_custom_arg="${1:-}";
        shift;
    fi
    
    # optional run-with-env args, of the form ...
    #     -u <env-name>
    #     -e <env>=<value>
    local _e_idx=0;
    local u_idx=0;
    while getopts u:e: opt "$@"; do
        case $opt in
            e)
                #echo "    parsing '-e' '${OPTARG}'";
                __gp_e_args[$_e_idx]="$OPTARG";
                _e_idx=$((_e_idx+1));
                ;; 
            u)
                #echo "    parsing '-u' '${OPTARG}'";
                __gp_u_args[$u_idx]="$OPTARG";
                u_idx=$((u_idx+1));
                ;;
            *)
                # Unexpected option, exit with status of last command
                exit $?
                ;;
        esac
        # remove the option from the cmdline
        shift $((OPTIND-1)); OPTIND=1
    done

    #
    # optionally clean up options delimiter '--' 
    #
    if [ "${1:-}" = "--" ]; then
        shift;
    fi

    # the remaining args are the module command line
    __gp_module_cmd=( "$@" );

    # process '-e' flags immediately
    setEnvironmentVariables;

    # process '-c' flag next
    __gp_env_custom_script=$(envCustomScript "${__gp_env_custom_arg}");
    if [[ ! -f "${__gp_env_custom_script}" ]]; then
        # it's not a regular file
        __gp_env_custom_script="";
    fi
    sourceSiteCustomizationScripts;

    ## load module runtime environments
    #loadModuleEnvs;
    addModuleEnvs;
    ##initModuleEnvs;
}

function parseEnvCustomArg() {
    # optional -c <__gp_env_custom_arg>
    if [ "${1:-}" = "-c" ]; then
        shift;
        __gp_env_custom_arg="${1}";
        shift;
    fi
}

# process '-e' flags of the form
#     -e ENV_VAR=VALUE
function setEnvironmentVariables() {
    if [ "${#__gp_e_args[@]}" != 0 ]; then
        local e_arg;
        #echo "processing '-e' args ...";
        for e_arg in "${__gp_e_args[@]}"
        do
            #echo "    e_arg: ${e_arg}";
            exportEnv "$e_arg";
        done
    fi
}

# 
# envCustomScript -- get the path to the optional 'env-custom' script.
#
# Options:
#    1) export GP_ENV_CUSTOM=<env-custom>; <run-with-env> ... 
#    2) <run-with-env> '-e' GP_ENV_CUSTOM=<env-custom>
#    3) <run-with-env> '-c' <env-custom>
# Default: env-custom.sh 
#
# Relative paths are loaded relative to the 'wrapper_scripts' directory.
# Absolute paths can also be set.
#
# When this references an actual file it is sourced as the site 
# customization script
#
function envCustomScript() {
    if ! [[ -z "${GP_ENV_CUSTOM:-}" ]]; then
        echo "$(convertPath ${GP_ENV_CUSTOM})";
        return;
    elif ! [[ -z "${1:-}" ]]; then
        echo "$(convertPath $1)";
        return;
    fi
    echo "$(convertPath 'env-custom.sh')";
}
#export -f envCustomScript;

function sourceSiteCustomizationScripts() {
    # load 'env-default.sh' script   
    source "${__gp_script_dir}/env-default.sh"
    # optionally load 'env-custom'
    #__gp_env_custom_script=$(envCustomScript "${__gp_env_custom_arg}");
    if [ -f "${__gp_env_custom_script}" ];
    then 
        source "${__gp_env_custom_script}";
    fi
}

function loadModuleEnvs() {
    addModuleEnvs;
    initModuleEnvs;
}

function addModuleEnvs() {
    # process '-u' args after loading env-custom scripts
    if [ "${#__gp_u_args[@]}" != 0 ]; then
        local u_arg;
        #echo "processing '-u' args ...";
        for u_arg in "${__gp_u_args[@]}"
        do
            #echo "    u_arg: ${u_arg}";
            addEnv "$u_arg";
        done
    fi
}

function initModuleEnvs() {
    if [ "${#_runtime_environments[@]}" != 0 ]; then
        for module in "${_runtime_environments[@]}"
        do
            initEnv "${module}"
        done
    fi  
}
