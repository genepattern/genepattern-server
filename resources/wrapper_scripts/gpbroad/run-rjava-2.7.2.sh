#!/usr/bin/env bash
#
# Wrapper script for running R-2.7.2 as a java command
# This also adds a required library to the LD_LIBRARY_PATH
#
# Set the path to the script in custom.properties:
#     R2.7.2_RJava=/xchip/gpbroad/wrapper_scripts/run-rjava-2.7.2.sh -cp <run_r_path> RunR
#
# Module command line:
#    commandLine=<R-2.7.2> ...
#
# Previous def'n of R-2.7.2:
#     R-2.7.2=<java> -DR_suppress=<R.suppress.messages.file> -DR_HOME=<R-2.7.2_HOME> -Dr_flags=<r_flags> -cp <run_r_path> RunR
#
. /broad/tools/scripts/useuse
use R-2.7
use Java-1.7
r=`which R`
rhome=${r%/*/*}

# Needed for FLAME modules
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/xchip/gpdev/shared_libraries"

java -DR_HOME=$rhome -Dr_flags='--no-save --quiet --slave --no-restore' "$@"
