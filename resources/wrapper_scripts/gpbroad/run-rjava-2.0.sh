#!/usr/bin/env bash
#
# Wrapper script for running R-2.0 as a java command
#
# This works for legacy modules with the <R> command line substitution parameter. For example, LogisticRegresion.
# commandLine=<R> ...
#
# You must define R as a custom property, either by editing custom.properties and restarting the server or by 
# directly added the property from the Admin->Server Settings->Custom page.
# R=/xchip/gpbroad/wrapper_scripts/run-rjava-2.0.sh -cp <run_r_path> RunR

. /broad/tools/scripts/useuse
use R-2.0
use Java-1.7
r=`which R`
rhome=${r%/*/*}
java -DR_HOME=$rhome -Dr_flags='--no-save --quiet --slave --no-restore' "$@"
