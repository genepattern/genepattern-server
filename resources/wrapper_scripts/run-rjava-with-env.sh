#!/usr/bin/env bash

############################################################
# run-rjava-with-env.sh
#   Run an 'Rjava' module using gp-common.sh library
# Alternative to run-rjava.sh
# 
# Usage:
#   run-rjava-with-env.sh \
#     -c env-custom \
#     -u java/1.8 -u r/2.15 -- \
#     [java-flags] \
#     -cp classpath \
#     RunR rscript-file r-main-function
# Example command line:
#   GP_DRY_RUN=true GP_DEBUG=true ../run-rjava-with-env.sh -c env-custom-macos.sh 
#     -u java/1.8 -u r/2.15 -- 
#     -cp ../../../website/WEB-INF/classes 
#     RunR hello.R hello
#
# Example custom.properties entry:
#   R2.15_Rjava=<wrapper-scripts>/run-rjava-with-env.sh \
#     -c <env-custom> -u Java-1.7 -u R-2.15 -- \
#     <rjava_flags> -cp <run_r_path> RunR
#
# References:
#   gp-common::parse_args
############################################################

############################################################
# Function: run_rjava_with_env
############################################################
function run_rjava_with_env() {
  log "parse_args..."
  parse_args "${@}"
  log "initModuleEnvs"
  initModuleEnvs
  # customization for run-rjava.sh script
  r=`which R`
  rhome=${r%/*/*}
  if is_debug; then
    echo "r=$r";
    echo "rhome=$rhome";
    echo "__gp_module_cmd: ${__gp_module_cmd[@]}";
  fi
  local java_cmd=( 
    "java" "-DR_HOME=${rhome}" -Dr_flags='--no-save --quiet --slave --no-restore' \
      "${__gp_module_cmd[@]}" );
  $DRY_RUN "${java_cmd[@]}"
}

main() {
  local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${__dir}/gp-common.sh"
  run_rjava_with_env "${@}"
}

main "${@}"
