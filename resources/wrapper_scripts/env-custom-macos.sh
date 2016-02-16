#!/usr/bin/env bash

#
# Custom runtime environment for MacOS X installation
#


#
# Initialize runtime environment. 
#     Usage: initEnv <runtime-env-name>
# initEnv will be called at module runtime for each required <runtime-env-name>
#
# See env-default.sh for canonical runtime environment names
#
function initEnv() {
    if ! [ -z ${GP_DEBUG+x} ]; then
        # only when the GP_DEBUG flag is set
        echo "loading $1 ..."
        echo R_LIBS_SITE=${R_LIBS_SITE}
    fi
    
    # set path for Java-1.7
    if [ "$1" = "Java-1.7" ]; then
        setjdk 1.7

    # set path for Java-1.8
    elif [ "$1" = "Java-1.8" ]; then
        setjdk 1.8

    # set path for R-3.1
    elif [ "$1" = "R-3.1" ]; then
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/3.1/Resources
        GP_SET_R_PATH=true;

    # set path for R-3.0
    elif [ "$1" = "R-3.0" ]; then
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/3.0/Resources
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
# helper function to switch version of java
#
# Usage: setjdk 1.7 
# Usage: setjdk 1.8 
#
# thanks JayWay, http://www.jayway.com/2014/01/15/how-to-switch-jdk-version-on-mac-os-x-maverick/
#
function setjdk() {
    if [ $# -ne 0 ]; then
        removeFromPath '/System/Library/Frameworks/JavaVM.framework/Home/bin'
        if [ -n "${JAVA_HOME+x}" ]; then
            removeFromPath $JAVA_HOME
        fi
        export JAVA_HOME=`/usr/libexec/java_home -v $@`
        export PATH=$JAVA_HOME/bin:$PATH
    fi
}

function removeFromPath() {
    export PATH=$(echo $PATH | sed -E -e "s;:$1;;" -e "s;$1:?;;")
}

