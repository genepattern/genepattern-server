#!/usr/bin/env bash

#
# declare default implementation for initializing a runtime environment
# Usage: initEnv <runtime-env-name>
#
function initEnv() {
    if ! [ -z ${GP_DEBUG+x} ]; then
        # only when the GP_DEBUG flag is set
        echo "loading $1 ..."
        echo R_LIBS_SITE=${R_LIBS_SITE}
    fi

    # set path for R-3.1
    if [ "$1" = "R-3.1" ]; then
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/3.1/Resources
        GP_SET_R_PATH=true;

    # set path for R-2.15
    elif [ "$1" = "R-2.15" ]; then
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/2.15/Resources
        GP_SET_R_PATH=true;
    fi

    # must opt-in to mkdirs
    if ! [ -z ${GP_MKDIR_R_LIBS_SITE+x} ]; then
        mkdir -p "${R_LIBS_SITE}"
    fi

    # add R_HOME/bin to the PATH
    if ! [ -z ${GP_SET_R_PATH+x} ]; then
        export PATH=${R_HOME}/bin:${PATH}
    fi

}

#
# canonical runtime environment names
#

#putValue 'R-2.0'
#putValue 'R-2.5'
#putValue 'R-2.7'
#putValue 'R-2.10'
#putValue 'R-2.11'
#putValue 'R-2.13'
#putValue 'R-2.14'
#putValue 'R-3.0'

