#!/usr/bin/env bash

############################################################
# run-rjava-with-env.sh
#   Alternative implementation of run-rjava.sh wrapper script,
# which uses more of the gp-common.sh library functions.
# 
# Configuration:
#   R2.15_Rjava=<wrapper-scripts>/run-rjava-with-env.sh \
#      -c <env-custom> -u Java-1.7 -u R-2.15 -- <rjava_flags> -cp <run_r_path> RunR
#
# Usage:
#   run-rjava-with-env.sh -c env-custom -u Java-1.7 -u R-2.5 -- java <java-flags> -cp <run_r_path> RunR <args>
# For testing/debugging
#   run-rjava-with-env.sh -c env-custom -u Java-1.7 -u R-2.5 -- java -version
#   ../run-rjava-with-env.sh -c env-custom-macos.sh -u Java-1.7 -u R-2.5 -- -cp ../../../website/WEB-INF/classes/ RunR hello.R hello
############################################################

############################################################
# Function: run_rjava_with_env
############################################################
function run_rjava_with_env() {
  parse_args "${@}"
  initModuleEnvs
  # debug: echoCmdEnv
  # debug: echo "which R: $(which R)";
  # customization for run-rjava.sh script
  r=`which R`
  # debug: echo "r=$r";
  rhome=${r%/*/*}
  # debug: echo "rhome=$rhome";
  # debug: echo "__gp_module_cmd: ${__gp_module_cmd[@]}";
  java "-DR_HOME=${rhome}" -Dr_flags='--no-save --quiet --slave --no-restore' "${__gp_module_cmd[@]}"
}

main() {
  local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${__dir}/gp-common.sh"
  run_rjava_with_env "${@}"
}

main "${@}"
