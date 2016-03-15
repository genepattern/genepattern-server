#!/usr/bin/env bash

##############################################################################
#
# wrapper script for stopping the GenePattern Server
# Usage:
#     nohup ./stop-genepattern.sh &
#
#     # optionally set server customization environment
#     nohup ./stop-genepattern.sh -c server-env-custom.sh &
#
# Environment variables:
#     GENEPATTERN_HOME
#     CATALINA_HOME
##############################################################################

_gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. "$_gp_script_dir/init-genepattern.sh"

"${_gp_script_dir}/run-with-env.sh" "$@" -u Java-1.8 "${CATALINA_HOME}/bin/catalina-macapp.sh" stop &
