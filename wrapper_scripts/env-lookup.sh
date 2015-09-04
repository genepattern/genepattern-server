#!/usr/bin/env bash

#
# Lookup table for module runtime environment names
#

# optionally set envMap to an array
# Each key is a canonical names for a module runtime environment,
# Each value is a site specific value, used to load that environment,
# For example, a DotKit name for a Broad hosted GP servers.
# the values are site-specific customizations

function _env_lookup_checkInit() { 
  # initialize new bash3 compatible hashmap
  if [ -z "${_env_hashmap_inited+1}" ]; then 
    initModuleEnv
  fi
}

#
# Utility function for getting the directory which contains this script
# It is relative to the script from which this function was called
# (Not from which it is defined)
#
function scriptDir() {
    local SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
    echo "$SCRIPT_DIR";
}

#
# if it's a relative path; return the full path relative to the script directory 
# Usage: initPath <path>
#
#
function initPath() {
    if [[ "$1" = /* ]]; then
        echo "$1";
    else
        local SCRIPT_DIR=$( scriptDir )
        echo "${SCRIPT_DIR}/$1"; 
    fi
}

function initModuleEnv() {
  source $(scriptDir)/env-hashmap.sh
  declare -a runtimeEnvironments=();
}

function addEnv() {
    u_arg="$1";
    
    oldIFS="$IFS";
    IFS=', '
    read -a actualArr <<< "$(getValue $u_arg)"
    IFS="$oldIFS"
    #echo "actualArr: ${actualArr[@]}"
    #assertEquals "parse custom values" "R-3.1,GCC-4.9" "$(join ',' ${actualArr[@]})"
    
    # step through each value
    for index in "${!actualArr[@]}"
    do
        runtimeEnvironments=( "${runtimeEnvironments[@]}" "${actualArr[index]}" )
    done
}

function initCanonicalValues() {
  # initialize the lookup table with canonical values 
  # Broad CentOS 5 config is almost canonical 
  putValue 'Java'
  putValue 'Java-1.7'
  putValue 'Matlab-2010b-MCR'  ### centos5 value '.matlab_2010b_mcr'
  putValue 'Matlab-2013a-MCR'  ### centos5 value '.matlab_2013a_mcr'
  putValue 'Python-2.5'
  putValue 'Python-2.6'
  putValue 'Python-2.7'
  putValue 'R-2.0'
  putValue 'R-2.5'
  putValue 'R-2.7'
  putValue 'R-2.10'
  putValue 'R-2.11'
  putValue 'R-2.13'
  putValue 'R-2.14'
  putValue 'R-2.15'
  putValue 'R-3.0'
  putValue 'R-3.1'
  putValue 'GCC-4.9'
}

#    ./env-custom.sh, if no other flags set
#    ./$1, if the first arg is a script name
#    $1, if the first arg is a full path
#    ./$GP_ENV_CUSTOM
# usage: initCustomValues [<custom_script_name>]
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

function initCustomValues() {
    local env_custom_script=$(initCustomValuePath "$@") 
    if [ -f "${env_custom_script}" ]
    then 
        source "${env_custom_script}"
    fi
}

function initValues() {
  initCanonicalValues
  initCustomValues
}


_env_lookup_checkInit;

#function loadEnvs() {
#    // first arg is the function, 
#}

# Broad RHEL6 config
#  ['Java']='Java-1.7'
#  ['Java-1.7']='java/1.7.0_51'
#   ['Matlab_2010b_MCR']='.matlab_2010b_mcr'
#   ['Matlab_2013a_MCR']='.matlab_2013a_mcr'
#  ['.matlab-2013a']='matlab/2013a'
#  ['Python-2.5']='Python-2.6'
#  ['Python-2.6']='python/2.6.9'
#  ['R-2.0']='.genepattern-internal-2.0.1'
#  ['R-2.5']='.genepattern-internal-2.5.1'
#  ['R-2.7']='.genepattern-internal-2.7.2'
#  ['R-2.10']='R/2.10.1'
#  ['R-2.11']='.genepattern-internal-2.11.0'
#  ['R-2.13']='R/2.13.2'
#  ['R-2.14']='R/2.14.2'
#  ['R-2.15']='R/2.15.2'
#  ['R-3.0']='R/3.0.1'
#  ['R-3.1']='R/3.1.1'
#  ['GCC']='GCC-4.9'


# IU config
#  ['Java-7']='java/1.7.0_51'
#  ['.matlab-2013a']='matlab/2013a'
#  ['Python-2.5']='python/2.5.4'
#  ['Python-2.6']='python/2.6.9'
#  ['R-2.5']='R/2.5.1'
#  ['R-2.7']='R/2.7.2'
#  ['R-2.10']='R/2.10.1'
#  ['R-2.11']='R/2.11.1'
#  ['R-2.13']='R/2.13.2'
#  ['R-2.14']='R/2.14.2'
#  ['R-2.15']='R/2.15.2'
#  ['R-3.0']='R/3.0.1'
#  ['R-3.1']='R/3.1.1'
#  ['GCC']='gcc/4.7.2'



#
# check for installation-specific 'module' name, and replace with necessary
#     e.g. replace '.matlab-2013a' with 'matlab/2013a' 
#
# args: $1, must be the name of the 'environment module', 
#     e.g. '.matlab-2013a'
#
# return: the inital $1 value, or it's substitution from the envMap associative-array,
#     e.g. 'matlab/2013a'
#
#function modOrReplace() {
#    local module="$1";
#
#    moduleReplace=${envMap[$module]};
#    if [ -n "${moduleReplace}" ]
#    then 
#        #debug: echo "replacing $module with $moduleReplace ... "
#        module=$moduleReplace
#    fi
#    echo $module;
#}
