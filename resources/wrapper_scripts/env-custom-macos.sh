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
    if is_debug; then
        # only when GP_DEBUG="true"
        echo "initEnv(): initializing '$1' environment ..."
    fi
    
    local GP_SET_R_PATH;

    case "${1}" in
      java/1.7|Java-1.7) 
        # set path for Java-1.7
        setjdk 1.7
        ;;

      java/1.8|Java-1.8)
        # set path for Java-1.8
        setjdk 1.8
        ;;

      r/3.3|R/3.3|r-3.3|R-3.3)
        # set path for R-3.3
        R_HOME=/Library/Frameworks/R.framework/Versions/3.3/Resources
        GP_SET_R_PATH="true";
        ;;

      r/3.2|R/3.2|r-3.2|R-3.2)
        # set path for R-3.2
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/3.2/Resources
        GP_SET_R_PATH="true";
        ;;

      r/3.1|R/3.1|r-3.1|R-3.1)
        # set path for R-3.1
        # add Rscript to path 
        export R_HOME=/Library/Frameworks/R.framework/Versions/3.1/Resources
        GP_SET_R_PATH="true";

        # note: you must edit your local R/3.1 installation 
        #   in order to work with multiple versions of R
        #   this is only strictly necessary if you install a newer (than 3.1)
        #   version of R on your Mac.
        #
        # use the included ./R/3.1/R_v3.1.3.patch file
        # 
        #   cd /Library/Frameworks/R.framework/Versions/3.1/Resources/bin
        #   # first, backup the existing R file
        #   cp R R.orig
        #   # then, apply the patch
        #   patch < R_v3.1.3.patch
        ;;

      r/3.0|R/3.0|r-3.0|R-3.0)
        # set path for R-3.0
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/3.0/Resources
        GP_SET_R_PATH="true";
        ;;

      r/2.15|R/2.15|r-2.15|R-2.15)
        # set path for R-2.15
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/2.15/Resources
        GP_SET_R_PATH="true";
        ;;

      r/2.5|R/2.5|r-2.5|R-2.5)
        # set path for R-2.5
        # add Rscript to path
        R_HOME=/Library/Frameworks/R.framework/Versions/2.5/Resources
        GP_SET_R_PATH="true";
        ;;

      r/2.0|R/2.0|r-2.0|R-2.0)
        # set path for R-2.0
        R_HOME=/Library/Frameworks/R.framework/Versions/2.0/Resources
        GP_SET_R_PATH="true";
        ;;
    esac

    # add R_HOME/bin to the PATH
    if [[ "${GP_SET_R_PATH:-}" = "true" ]]; then
        if is_debug; then
            echo "initEnv():      adding '${R_HOME}/bin' to the PATH ..."
        fi
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

