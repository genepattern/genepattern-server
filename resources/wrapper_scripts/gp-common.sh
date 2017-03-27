#!/usr/bin/env bash

############################################################
# gp-common.sh
#
#   Common bash functions for GenePattern wrapper scripts
#   Must be sourced rather than executed.
#
# Requirements:
#   * Must work with Bash 3 for Mac OS X compatibility
#     Tested with version 3.2.57(1)-release (x86_64-apple-darwin15)
#   * For best results do not use symbolic links to this script
#     or other executables. As a workaround set GP_SCRIPT_DIR before
#     sourcing this script.
# 
# Includes:
#   env-hashmap.sh - bash3 compatible alternative to an associative array
#
# Globals:
#   GP_SCRIPT_DIR, set the fully qualified path to the wrapper-scripts directory
#   GP_ENV_CUSTOM, set the name or path to the env-custom file
#
# Declared variables:
#   GP_SCRIPT_DIR - by default it is the parent dir of gp-common.sh
#   __gp_env_custom_script - the path to the site customization file
#   __gp_module_envs - the list of required runtime environments, aka dotkits
#   __gp_module_cmd - the command to run
#
# Declared functions
#   exportEnvs        process '-e' flags, set environment variables
#   sourceEnvScripts  process -c' flag, source site-customization file(s)
#   addModuleEnvs     process '-u' flags, initialize module environments
#   initModuleEnvs    load module environments
#   parseArgs         parse the wrapper command line args
#   run               all steps necessary to run the module command line
#
#   (helpers)
#   parseOpt
#   exportEnv key=[value]
#   (included in env-hashmap.sh)
#   putValue canonical-name [local-name][,local-name]*
#
# Note: paths are relative to ${GP_SCRIPT_DIR}, e.g.
#     '-c' 'env-custom-macos.sh'
#     source ${GP_SCRIPT_DIR}/env-custom-macos.sh
# 
# See more details in the 'run-with-env.sh' comments.
############################################################

# include this script once and only once
if [[ "${__gp_inited:-}" -eq 1 ]]; then 
    return;
else
    readonly __gp_inited=1;
    # set GP_SCRIPT_DIR, unless it's already been set
    if [ -z "${GP_SCRIPT_DIR:-}" ]; then
        # default, fully qualified path to bash_source dir
        readonly GP_SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
    fi
    readonly __gp_env_default_script="${GP_SCRIPT_DIR}/env-default.sh";
    source "${GP_SCRIPT_DIR}/env-hashmap.sh"
    
fi

declare __gp_env_custom_arg="";
declare __gp_env_custom_script="";
# placeholder for '-e' args
declare -a __gp_e_args=();
# placeholder for '-u' args
declare -a __gp_u_args=();
# the module command line, stripped of configuration args such as '-c', '-e', and '-u'
declare -a __gp_module_cmd=();

############################################################
# parseArgs, parse command line args, initialize
#   environment variables and site customization scripts
#
# Parse run-with-env command line args
#     input: ${@}, the command line to the script
#     output: an updated command line array, with run-with-env params stripped.
#
# Usage:
#   parseArgs [-c env-custom] [-e key[=[val]]]* [-u module-name]* \
#     [--] module_cmd [module_arg]*
# Input: 
#   -c env-custom, optional, path to site customization script
#   -e key=value, optional list of environment variables to set
#   -u module-name, option list of module environments (aka dotkits) to load
#   --, optional options delimiter to split the args to this wrapper
#     script from the actual module command line
#   module_cmd, required, the executable
#   module_args, optional, the list of module command line arguments
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
############################################################
function parseArgs() {
    __gp_env_custom_arg="";
    __gp_env_custom_script="";
    __gp_e_args=(); 
    __gp_u_args=();
    __gp_module_cmd=();

#    # optional '-c <env-custom>' flag
#    if [[ "${1:-}" = "-c" ]]; then
#        shift; 
#        # only set if '-c' has an argument
#        #     [[ -z "${1+x}" ]] means next arg, $1, is not set
#        #     [[  $1 = -*  ]] means next arg starts with '-'
#        if ! [[ -z "${1+x}" || $1 = -* ]]; then
#            __gp_env_custom_arg="${1:-}";
#            shift;
#        fi
#    fi
        
    # optional run-with-env args, of the form ...
    #     -u <env-name>
    #     -e <env>=<value>
    local _e_idx=0;
    local u_idx=0;
    # reset OPTIND
    OPTIND=1
    while getopts c:u:e: opt "$@"; do
        # debug: echo "opt=${opt}, OPTIND=${OPTIND}, OPTARG=${OPTARG}";
        case "$opt" in
            c)
                # optional '-c <env-custom>' flag
                # debug: echo "    parsing '-c' '${OPTARG}'";
                if hasArg; then
                    __gp_env_custom_arg="$OPTARG";
                    #echo "    parsing '-c' '${OPTARG}', setting __gp_env_custom_arg=${__gp_env_custom_arg}";
                fi
                ;;
            e)
                # debug: echo "    parsing '-e' '${OPTARG}'";
                __gp_e_args[$_e_idx]="$OPTARG";
                _e_idx=$((_e_idx+1));
                ;; 
            u)
                # debug: echo "    parsing '-u' '${OPTARG}'";
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

    # remove options delimiter '--' 
    if [[ "${1:-}" = "--" ]]; then
        shift;
    fi

    # the remaining args are the module command line
    __gp_module_cmd=( "$@" );

    # process '-e' flags, set environment variables
    exportEnvs;

    # process '-c' flag, source site-customization file(s)
    sourceEnvScripts
#    __gp_env_custom_script="$(convertPath "${GP_ENV_CUSTOM:-${__gp_env_custom_arg:-env-custom.sh}}")";
#    # load 'env-default.sh' script
#    if [ -f "${__gp_env_default_script}" ]; then
#        source "${__gp_env_default_script}";
#    fi
#    # optionally load 'env-custom'
#    if [ -f "${__gp_env_custom_script}" ];
#    then 
#        source "${__gp_env_custom_script}";
#    fi

    # process '-u' flags, initialize module environments
    addModuleEnvs;
}

############################################################
# Function: hasArg, check for missing arg in getopts loop
# Output:
#   return 0, success, expected, when there is an arg
#   return 1, failure, unexpected, when the arg is missing
# Usage:
#   hasArg 
# Example:
#   if hasArg; then
#     env_custom="${OPTARG:-}"
#   fi
#
# This is useful for processing command line options which should
# have an argument, but which may not, e.g.
#     <run-with-env> -c -u java/1.8 java ..., missing -c arg
#     <run-with-env> -a -u java/1.8 java ..., missing -a arg
############################################################
hasArg() {        
  # only set if the current option has an argument, e.g.
  #   -c env-custom-macos.sh  
  #     [[ -z "${OPTARG+x}" ]] means next arg is not set
  #     [[  $OPTARG = -*  ]] means next arg starts with '-'
  if ! [[ -z "${OPTARG+x}" || $OPTARG = -* ]]; then
    # success
    return 0;
  else
    # failure
    #   reset OPTIND to '2', can be overridden with $1 arg 
    OPTIND=${1:-2};
    return 1;
  fi
}

############################################################
# exportEnv, export environment variable from command line
#   argument of the form '-e' 'key_value_pair'
# split the arg into a key and value, then export. 
#   'export key=value'
# Usage:
#   exportEnv key[=[value]]
# Input:
#   key_value_pair, required
#
# special-cases for 'arg'
#   'KEY='    (equals no value)     calls 'unset MY_KEY'
#   'MY_KEY'  (no value, no equals) calls 'export MY_KEY='
#   '=MY_VAL' (no key), ignored
#   ''        (no arg), ignored
############################################################
function exportEnv() {
    if [[ -z "${1:-}" ]]; then
        # short-circuit, missing required arg
        return;
    fi

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

############################################################
# Function: exportEnvs 
#   export all '-e' key=value command line args
# Usage:
#   exportEnvs
# Must call this after processing command line args.
############################################################
function exportEnvs() {
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

############################################################
# Function: setEnvCustomScript
#
#   Sets the '__gp_env_script_dir' as a fully qualified path
# to the site customization file
#
# Usage:
#   setEnvCustomScript [filename]
# Input: filename, optional, default='env-custom.sh'
#    Customization:
#    1) export GP_ENV_CUSTOM=<env-custom>; <run-with-env> ... 
#    2) <run-with-env> '-e' GP_ENV_CUSTOM=<env-custom>
#    3) <run-with-env> '-c' <env-custom>
#    4) pass in an arg for debugging
#
# Relative paths are loaded relative to the 'wrapper_scripts' directory.
# Absolute paths can also be set.
#
# When this references an actual file it is sourced as the site 
# customization script
#
############################################################
function setEnvCustomScript() {
  __gp_env_custom_script="$(convertPath \
    "${GP_ENV_CUSTOM:-${1:-env-custom.sh}}")";
}

############################################################
# Function: sourceEnvScripts
#   Source the default and custom configuration scripts
# References:
#   __gp_env_default_script, default=<wrapper-scripts>/env-default.sh
#   __gp_env_custom_script,  default=<wrapper-scripts>/env-custom.sh
############################################################
function sourceEnvScripts() {
    # process '-c' flag next
    setEnvCustomScript "${__gp_env_custom_arg:-}";
    # load 'env-default.sh' script
    if [ -f "${__gp_env_default_script}" ]; then
        source "${__gp_env_default_script}";
    fi
    # optionally load 'env-custom'
    if [ -f "${__gp_env_custom_script}" ];
    then 
        source "${__gp_env_custom_script}";
    fi
}

############################################################
# addModuleEnvs, add items to the __gp_module_envs array
# calls 'addEnv' for each requested environment
# Usage:
#   addModuleEnvs
# Inputs: No args
# References:
#   __gp_u_args, the list of '-u' command line args
# 
# handles site-customization 
# calls 'addEnv' for each '-u module_env' command line arg 
# process '-u' flags of the form
#     '-u' module_env
# must call this after 'exportEnvs'
# and loading the site-customization file
############################################################
function addModuleEnvs() {
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

############################################################
# initModuleEnvs, initialize module runtime environments
# Usage:
#   initModuleEnvs
# Input args: No args
# Input references: 
#   __gp_module_envs is initialied in 'addModuleEnvs' 
############################################################
function initModuleEnvs() {
    if [ "${#__gp_module_envs[@]}" != 0 ]; then
        for module in "${__gp_module_envs[@]}"
        do
            initEnv "${module}"
        done
    fi  
}

############################################################
# convertPath, Convert relative path to fully qualified
# path, if necessary
# Usage: 
#   convertPath (<relativePath> | <fullyQualifiedPath>)
############################################################
function convertPath() {
    local arg1=${1:-};
    if [[ "${arg1}" = /* ]]; then
        echo "$arg1";
    else
        echo "${GP_SCRIPT_DIR}/$arg1"; 
    fi
}

############################################################
# extractRootName, helper function, get the root name from 
# the full moduleName.
# Usage: 
#   extractRootName moduleName
# Example:
#    echo extractRootName "R/2.15.3"
#    > "R"
#
function extractRootName() {
    rootName="${1%%\/*}";
    echo "$rootName";
}

############################################################
# for debugging, echo variables
#   note, not for production, can be helpful for debugging
# after parseArgs
############################################################
function echoEnv() {
    echo "                GP_DEBUG: ${GP_DEBUG:- (not set)}";
    echo "           GP_SCRIPT_DIR: ${GP_SCRIPT_DIR:-}";
    echo "           GP_ENV_CUSTOM: ${GP_ENV_CUSTOM:- (not set)}";
    echo "     __gp_env_custom_arg: ${__gp_env_custom_arg:- (not set)}";
        echo " __gp_env_default_script: ${__gp_env_default_script:- (not set)}";
    echo "  __gp_env_custom_script: ${__gp_env_custom_script:- (not set)}";
    echo "             __gp_e_args: ${__gp_e_args[@]:- (none)}";
    echo "             __gp_u_args: ${__gp_u_args[@]:- (none)}";
        echoEnvMap "            ";
    echo "        __gp_module_envs: ${__gp_module_envs[@]:- (none)}"
    echo "         __gp_module_cmd: ${__gp_module_cmd[@]:- (not set)}"
}

############################################################
# run the command line
# call this from an executable shell script
# see run-with-env.sh for an example
#
# Usage:
#   local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
#   source "${__dir}/gp-common.sh"
#   run "${@}"
############################################################
function run() {
    parseArgs "${@}";
    initModuleEnvs;
    "${__gp_module_cmd[@]}";
}


