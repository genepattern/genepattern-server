#!/usr/bin/env bash
#
# Wrapper script for running R-2.14 as a java command
#
#
. /broad/tools/scripts/useuse
use R-2.14
use Java-1.7
r=`which R`
rhome=${r%/*/*}
java -DR_HOME=$rhome -Dr_flags='--no-save --quiet --slave --no-restore' "$@"
