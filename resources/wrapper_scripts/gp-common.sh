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
#   * For best results avoid symbolic links to this script; 
#     as a workaround set GP_SCRIPT_DIR
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
#   export_envs         process '-e' flags, set environment variables
#   source_env_scripts  process -c' flag, source site-customization file(s)
#   add_module_envs     process '-u' flags, initialize module environments
#   init_module_envs    load module environments
#   parse_args          parse the wrapper command line args
#   run                 all steps necessary to run the module command line
#
#   (helpers)
#   export_env key=[value]
#   (included in env-hashmap.sh)
#   putValue canonical-name [local-name][,local-name]*
#
# Note: paths are relative to ${GP_SCRIPT_DIR}, e.g.
#     '-c' 'env-custom-macos.sh'
#     source ${GP_SCRIPT_DIR}/env-custom-macos.sh
# 
# Set 'site customization' in one of the following ways ...
#   with -c arg
#     run-with-env.sh -c env-custom ...
#   with environment variable
#     GP_ENV_CUSTOM=env-custom ; run-with-env.sh ...
#   with -e arg
#     run-with-env.sh -e GP_ENV_CUSTOM=env-custom ...
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

declare DRY_RUN="";
declare __gp_env_custom_arg="";
declare __gp_env_custom_script="";
# placeholder for '-e' args
declare -a __gp_e_args=();
# placeholder for '-u' args
declare -a __gp_u_args=();
# the module command line, stripped of configuration args such as '-c', '-e', and '-u'
declare -a __gp_module_cmd=();

############################################################
# strict_mode
#   set bash 'strict mode' flags
############################################################
strict_mode() {
  # Exit on error. Append || true if you expect an error.
  set -o errexit
  # Exit on error inside any functions or subshells.
  set -o errtrace
  # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
  set -o nounset
  # Catch the error in case mysqldump fails (but gzip succeeds) in `mysqldump |gzip`
  set -o pipefail
  # Turn on traces, useful while debugging but commented out by default
  #set -o xtrace
}

############################################################
# log
#   for debugging, print log message, by default 'echo'
############################################################
log() {
  if is_true "GP_DEBUG"; then
    echo "$@"
  fi
}

############################################################
# is_set
#   return 0, true, if the variable is set
#          1, false otherwise
# Usage:
#   is_set var-name
############################################################
is_set() {
  if [[ $# -eq 0 ]]; then
    return 1;
  fi
  local name="${1}";
  if [[ -z "${!name+x}" ]]; then
    return 1
  fi
  return 0;
}

# not_empty
not_empty() {
  local -r name="$1"
  [[ ! -z "${!name+x}"  && -n "${!name}" ]]
}

############################################################
# not_set
############################################################
not_set() {
  # [[ -z "${R_LIBS_SITE+x}" ]]
  if [[ $# -eq 0 ]]; then
    return 1;
  fi
  local name="${1}";
  [[ -z "${!name+x}" ]]
}

############################################################
# is_empty
#   return 0, true, if the variable is set to an empty string
#          1, false otherwise
# If the variable is not set, then it is not empty
############################################################
is_empty() {
  if [[ $# -eq 0 ]]; then
    return 1;
  fi
  local name="${1}";
  # must be 'set' to be empty
  if [[ -z "${!name+x}" ]]; then
    return 1
  fi
  [[ -z "${!name-}" ]]
}

############################################################
# is_true
#   return 0 if the variable referenced by var-name is true
#   return 1 if it is not set or false
# Usage:
#   if is_true var-name; then
#     ...
#   fi
# By def'n, true means the variable is set to one of ...
#   0 | true | TRUE
#
# This uses an indirect reference to the named variable
############################################################
is_true() {
  if [[ $# -eq 0 ]]; then
    # debug echo "missing arg"
    return 1;
  fi

  # variable name
  local name="${1}";
  if [[ -z "${!name+x}" ]]; then
    return 1
  fi
  local val="${!name}";
  if [[ "${val}" = 0 || "${val}" = "true" || "${val}" = "TRUE" ]]; then
    return 0;
  fi
  return 1
}

############################################################
# is_debug
#   convenience method to check if GP_DEBUG is set
# Usage:
#   if is_debug; then
#     log "debugging ..."
#   fi
############################################################
is_debug() {
  if is_true "GP_DEBUG"; then
    return 0
  fi
  return 1
  # return is_true "GP_DEBUG"
}

############################################################
# Function: is_valid_var
#   Is the arg a valid Bash variable name
# Output:
#   return 1 for false
#   return 0 for true
# Usage:
#   if is_valid_var "var_name"; then
#      ...
#   fi
############################################################
is_valid_var() {
  if [[ $# -eq 0 ]]; then
    # debug echo "missing arg"
    return 1;
  fi

  # Invalid bash variable
  [[ ! "$1" =~ ^[a-zA-Z_]+[a-zA-Z0-9_]*$ ]] && { 
    # debug: echo "Invalid bash variable: $1" 1>&2 ; 
    return 1 ; 
  }
  return 0;
}

############################################################
# Function: is_valid_r_version
############################################################
is_valid_r_version() {
  if [[ $# -eq 0 ]]; then
    return 1;
  fi

  # must be a valid R version, of the form x.y.x
  [[ ! "$1" =~ ^[0-9]+[0-9.]*$ ]] && { 
    # debug: echo "Invalid R version: $1" 1>&2 ; 
    return 1 ; 
  }
  return 0;
}

############################################################
# Function: has_arg, check for missing arg in getopts loop
# Output:
#   return 0, success, expected, when there is an arg
#   return 1, failure, unexpected, when the arg is missing
# Usage:
#   has_arg [optind, default=2]
# Example:
#   if has_arg; then
#     env_custom="${OPTARG:-}"
#   fi
#
# This is useful for processing command line options which should
# have an argument, but which may not, e.g.
#     <run-with-env> -c -u java/1.8 java ..., missing -c arg
#     <run-with-env> -a -u java/1.8 java ..., missing -a arg
############################################################
has_arg() {        
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
# parse_args, parse command line args, initialize
#   environment variables and site customization scripts
# Usage:
#   parse_args [-c env-custom] [-e key[=[val]]]* [-u module-name]* \
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
# Examples:
#     parse_args '-c' 'env-custom-macos.sh' ...
#     parse_args -e GP_ENV_CUSTOM=env-custom-macos.sh ...
#     GP_ENV_CUSTOM=env-custom-macos.sh ; parse_args ...
############################################################
parse_args() {
    __gp_env_custom_arg="";
    __gp_env_custom_script="";
    __gp_e_args=(); 
    __gp_u_args=();
    __gp_module_cmd=();
        
    # optional run-with-env args, of the form ...
    #     -u <env-name>
    #     -e <env>=<value>
    local _e_idx=0;
    local _u_idx=0;
    # reset OPTIND, for testing
    OPTIND=1
    while getopts c:u:e: opt "$@"; do
        # debug: echo "opt=${opt}, OPTIND=${OPTIND}, OPTARG=${OPTARG}";
        case "$opt" in
            c)
                # optional '-c <env-custom>' flag
                # debug: echo "    parsing '-c' '${OPTARG}'";
                if has_arg; then
                    __gp_env_custom_arg="$OPTARG";
                    # debug: echo "    parsing '-c' '${OPTARG}', setting __gp_env_custom_arg=${__gp_env_custom_arg}";
                fi
                ;;
            e)
                # debug: echo "    parsing '-e' '${OPTARG}'";
                __gp_e_args[$_e_idx]="$OPTARG";
                _e_idx=$((_e_idx+1));
                ;; 
            u)
                # debug: echo "    parsing '-u' '${OPTARG}'";
                __gp_u_args[$_u_idx]="$OPTARG";
                _u_idx=$((_u_idx+1));
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
    export_envs;

    # process '-c' flag, source site-customization file(s)
    source_env_scripts

    # process '-u' flags, initialize module environments
    add_module_envs;
    
    # optionally set the DRY_RUN variable
    init_dry_run;
}

############################################################
# export_env, export environment variable from command line
#   argument of the form '-e' 'key_value_pair'
# split the arg into a key and value, then export. 
#   'export key=value'
# Usage:
#   export_env key[=[value]]
# Input:
#   key_value_pair, required
#
# special-cases for 'arg'
#   'KEY='    (equals no value)     calls 'unset MY_KEY'
#   'MY_KEY'  (no value, no equals) calls 'export MY_KEY='
#   '=MY_VAL' (no key), ignored
#   ''        (no arg), ignored
############################################################
function export_env() {
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
# Function: export_envs 
#   export all '-e' key=value command line args
# Usage:
#   export_envs
# Must call this after processing command line args.
############################################################
function export_envs() {
    if [ "${#__gp_e_args[@]}" != 0 ]; then
        local e_arg;
        #echo "processing '-e' args ...";
        for e_arg in "${__gp_e_args[@]}"
        do
            #echo "    e_arg: ${e_arg}";
            export_env "$e_arg";
        done
    fi
}

############################################################
# Function: set_env_custom_script
#
#   Sets the '__gp_env_script_dir' as a fully qualified path
# to the site customization file
#
# Usage:
#   set_env_custom_script [filename]
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
function set_env_custom_script() {
  __gp_env_custom_script="$(convertPath \
    "${GP_ENV_CUSTOM:-${1:-env-custom.sh}}")";
}

############################################################
# Function: source_env_scripts
#   Source the default and custom configuration scripts
# References:
#   __gp_env_default_script, default=<wrapper-scripts>/env-default.sh
#   __gp_env_custom_script,  default=<wrapper-scripts>/env-custom.sh
############################################################
function source_env_scripts() {
    # process '-c' flag next
    set_env_custom_script "${__gp_env_custom_arg:-}";
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
# add_module_envs, add items to the __gp_module_envs array
# calls 'addEnv' for each requested environment
# Usage:
#   add_module_envs
# Inputs: No args
# References:
#   __gp_u_args, the list of '-u' command line args
# 
# handles site-customization 
# calls 'addEnv' for each '-u module_env' command line arg 
# process '-u' flags of the form
#     '-u' module_env
# must call this after 'export_envs'
# and loading the site-customization file
############################################################
function add_module_envs() {
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
# init_module_envs, initialize module runtime environments
# Usage:
#   init_module_envs
# Input args: No args
# Input references: 
#   __gp_module_envs is initialied in 'add_module_envs' 
############################################################
function init_module_envs() {
    if [ "${#__gp_module_envs[@]}" != 0 ]; then
        for module in "${__gp_module_envs[@]}"
        do
            initEnv "${module}"
        done
    fi  
}

############################################################
# init_dry_run
#   set DRY_RUN variable based on GP_DRY_RUN flag
# Usage:
#   Set the GP_DRY_RUN flag as a command line arg
#     ./run-with-env.sh ... -e GP_DRY_RUN=true
# Use the $DRY_RUN variable when executing commands,
#   $DRY_RUN my_cmd $my_args ...
############################################################
function init_dry_run() {
  if is_true "GP_DRY_RUN"; then
    DRY_RUN="echo"
  else
    DRY_RUN=""
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
# after parse_args
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
# for debugging, echo command and path
############################################################
function echoCmdEnv() {
    echo cmd="${__gp_module_cmd[@]}"
    local a=${__gp_module_cmd[0]:-""};
    if ! [[ -z ${a:-} ]]; then
      echo "which $a: $(which $a)";
    fi
    #echo "which Rscript: $(which Rscript)"
    echo "PATH: ${PATH}"
}

############################################################
# Function: run_with_env
#   Parse run-with-env args, initialize the environment,
#   and execute the command.
#
# Call this function from an executable shell script.
# Example command line call:
#   run-with-env.sh -c env_custom_macosh.sh -u java/1.8 java -version
# Example function call, wrapped in a main function in run-with-env.sh:
#   main() {
#     local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
#     source "${__dir}/gp-common.sh"
#     run_with_env "${@}"
#   }
#   main "${@}"
# See run-with-env.sh for more documentation
############################################################
function run_with_env() {
    parse_args "${@}";
    init_module_envs;
    $DRY_RUN "${__gp_module_cmd[@]}";
}

