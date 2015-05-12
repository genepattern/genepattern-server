#!/usr/bin/env bash

#
# Site specific wrapper environment for GenePattern Server
# installation at Indiana University
#

declare -a envCmd
envCmd=("module" "load")
#debug: 
envCmd=("echo" "loading module")

# declare associative array; requires bash v 4
# bash --version
# the keys are the DotKit values for Broad hosted GP servers
# the values are site-specific alternatives
declare -A envMap
envMap=( 
  ['Java-7']='java/1.7.0_51'
  ['.matlab-2013a']='matlab/2013a'
  ['Python-2.5']='python/2.5.4'
  ['Python-2.6']='python/2.6.9'
  ['R-2.15']='R/2.15.2'
  ['R-2.13']='R/2.13.2'
)

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
function initEnv() {
    if [ -z "$1" ]
    then
        return -1
    else 
        module="$1";
    fi
    "${envCmd[@]}" "${module}"
    return 0
}

#
# > R 2.15
# R/2.15.2

# > R 3.1 (Rscript)
# gcc/4.7.2
# R/3.1.1

# > R 2.14 (needs to be able to use both Rscript & R java scripts)
# java/1.7.0_51
# R/2.14.2

# > R 2.7 (needs to be able to launch with java)
# java/1.7.0_51
# R/2.7.2

# > R 3.0 (Rscript)
# gcc/4.7.2
# R/3.0.1

# > R 2.10 (needs to be able to use both Rscript & R java scripts)
# java/1.7.0_51
# R/2.10.1

# > R 2.13 (both Rscript and R java methods)
# module load java/1.7.0_51
# module load R/2.13.2

# > R 2.11 (java method)
# module load java/1.7.0_51
# module load R/2.11.1

