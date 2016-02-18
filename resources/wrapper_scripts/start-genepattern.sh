#!/usr/bin/env bash

##############################################################################
#
# wrapper script for starting the GenePattern Server
# Usage:
#     nohup ./start-genepattern.sh &
#
#     # optionally set server customization environment
#     nohup ./start-genepattern.sh -c server-env-custom.sh &
#
# Environment variables:
#     GENEPATTERN_HOME
#     CATALINA_HOME
##############################################################################

_gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
echo "_gp_script_dir=${_gp_script_dir}"
. "${_gp_script_dir}/init-genepattern.sh"

"${_gp_script_dir}/run-with-env.sh" "$@" -u Java-1.8 -u UGER "${CATALINA_HOME}/bin/catalina-macapp.sh" run &
