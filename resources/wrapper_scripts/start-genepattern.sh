#!/usr/bin/env bash

##############################################################################
#
# wrapper script for starting the GenePattern Server as a background process
# Usage:
#     ./start-genepattern.sh
#
#     # optionally set server customization environment
#     ./start-genepattern.sh -c server-env-custom.sh
#
# Options:
# -c <env-custom-site.sh>, optionally set a site customization file
#     case 1: use default
#         No need for '-c' flag, default environments are loaded
#     case 2: 'env-custom.sh'
#         No need for '-c' flag, 'env-custom.sh' is automatically loaded when it is present
#     case 3: non-standard site customization
#         -c env-custom-macos.sh
#     case 4: use a different site customization for the GP server process 
#         -c server-env-custom.sh
# More details in 'run-with-env.sh'
#
# Environment variables:
#     GENEPATTERN_HOME
#     CATALINA_HOME
##############################################################################

_gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
. "$_gp_script_dir/init-genepattern.sh"

#
# (alternative): hard-coded environment initialization
#
#. /broad/software/scripts/useuse
#use Java-1.8
#use UGER
#"${CATALINA_HOME}/bin/catalina-macapp.sh" run &

#
# wrapper script environment initialization
# Works best with default site customization file, 'env-custom.sh'
# Otherwise, you must pass in the name (or fq path) to the site customization file,
#    ./start-genepattern.sh -c env-custom-macos.sh ...
#
nohup "${_gp_script_dir}/run-with-env.sh" "$@" -u Java-1.8 -u .lsf-7.0 -u UGER "${CATALINA_HOME}/bin/catalina-macapp.sh" run &
