#!/usr/bin/env bash

#
# Site specific customizations for wrapper environment for GenePattern Server
# installation at Indiana University
#

#
# Helper function
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

function initMcr2013a() {
        # old way is missing jre config
        #export LD_LIBRARY_PATH="/N/soft/rhel6/matlab/MATLAB_Compiler_Runtime/v81/runtime/glnxa64:/N/soft/rhel6/matlab/MATLAB_Compiler_Runtime/v81/bin/glnxa64:/N/soft/rhel6/matlab/MATLAB_Compiler_Runtime/v81/sys/os/glnxa64:${LD_LIBRARY_PATH}"
        #return 0;

        # new way ...
        # define path to root MCR dir
        _mcr_root="/N/soft/rhel6/matlab/MATLAB_Compiler_Runtime/v81";
        # build MCR_LD_LIB_PATH elements
        _mcr_path=$(appendPath "${_mcr_path}" "${_mcr_root}/runtime/glnxa64")
        _mcr_path=$(appendPath "${_mcr_path}" "${_mcr_root}/bin/glnxa64")
        _mcr_path=$(appendPath "${_mcr_path}" "${_mcr_root}/sys/os/glnxa64")
        _mcr_path=$(appendPath "${_mcr_path}" "${_mcr_root}/sys/java/jre/glnxa64/jre/lib/amd64/native_threads")
        _mcr_path=$(appendPath "${_mcr_path}" "${_mcr_root}/sys/java/jre/glnxa64/jre/lib/amd64/server")
        _mcr_path=$(appendPath "${_mcr_path}" "${_mcr_root}/sys/java/jre/glnxa64/jre/lib/amd64")
        # define new ld_lib_path
        _ld_lib_path=$(appendPath "${LD_LIBRARY_PATH}" "${_mcr_path}")
        export LD_LIBRARY_PATH="${_ld_lib_path}"

        _xapplresdir=$(appendPath "${XAPPLRESDIR}" "${_mcr_root}/X11/app-defaults")
        export XAPPLRESDIR="${_xapplresdir}"
        return 0;
}


function initEnv() {
    # extract root package name, e.g. "R" from "R/2.15.3"
    package="${1%%\/*}";
    module unload "$package" &>/dev/null
    module load "$1" &>/dev/null

    # check exit code and fail if there are any problems
    # Note: as an improvement I'd like to capture output to a log file 
    #     rather than swallow to /dev/null
    if [ $? -ne 0 ]; then
        >&2 echo "Error loading $1, contact the server admin";
        exit $1
    fi
}

#declare -A envMap
envMap=( 
  ['Java']='java/1.7.0_51'
  ['Java-7']='java/1.7.0_51'
  # map canonical matlab 2013a MCR to IU module name
  ['Matlab-2013a-MCR']='matlab/2013a-mcr'
  # special-case, alias Matlab-2013a to Matlab-2013a-MCR
  ['Matlab-2013a']='matlab/2013a-mcr'
  ['.matlab-2013a']='matlab/2013a-mcr'
  ['Python-2.5']='python/2.5.4'
  ['Python-2.6']='python/2.6.9'
  ['Python-2.7']='python/2.7.3'
  ['R-2.5']='R/2.5.1'
  ['R-2.7']='R/2.7.2'
  ['R-2.10']='R/2.10.1'
  ['R-2.11']='R/2.11.1'
  ['R-2.13']='R/2.13.2'
  ['R-2.14']='R/2.14.2'
  ['R-2.15']='R/2.15.3'
  ['R-3.0']='R/3.0.1'
  ['R-3.1']='R/3.1.1'
  ['GCC']='gcc/4.7.2'
)
