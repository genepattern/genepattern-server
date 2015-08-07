#!/usr/bin/env bash

#
# Initialize command wrapper scripts for GenePattern Server
# Add a 'env-custom.sh' script to this folder in order to 
# customize for your installation
#


#
# The initEnv function is the site-specific method for initializing the environment for the
# given 'module', aka 'package', aka 'DotKit name'
#
# DotKit: use "$1" &>/dev/null
# Environment Modules: module load "$1" &>/dev/null
# debug: echo "loading module" $1
#
function initEnv() {
    echo "loading module" "$1"
}

# optionally set envMap to an associative array (requires bash v. 4)
# the keys are the DotKit values for Broad hosted GP servers
# the values are site-specific customizations
declare -A envMap
envMap=()

if [ -e $(dirname $0)/env-custom.sh ]
then 
    source $(dirname $0)/env-custom.sh
fi

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
function modOrReplace() {
    local module="$1";

    moduleReplace=${envMap[$module]};
    if [ -n "${moduleReplace}" ]
    then 
        #debug: echo "replacing $module with $moduleReplace ... "
        module=$moduleReplace
    fi
    echo $module;
}

#
# initialize the environment for the given 'environment module'
# args: $1, must be the name of the 'environment module', e.g. 'Java-7'
#
#function initEnv() {
#    if [ -z "$1" ]
#    then
#        return -1
#    else 
#        module="$1";
#    fi
#    #"${envCmd[@]}" "${module}"
#    envCmd "$module"
#    return 0
#}
