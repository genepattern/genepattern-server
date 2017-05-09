#!/usr/bin/env bash

############################################################
# run-rscript-pkgs.sh
#   Run an 'Rscript' module, installing R packages if necessary
# Usage:
#   ./run-rscript-pkgs.sh \
#     [--dry-run] \
#     [--debug] \
#     -v r-version \
#     -c env-custom \
#     -e name=value ...
#     --
#     rscript-args
#
# Environment variables passed in as -e name=value args ...
# ... for debugging
#   -e GP_DRY_RUN=true
#   -e GP_DEBUG=true
# See r-common.sh for the rest of the list
#
# References:
#   gp-common::parse_args
#   r-common::export_r_env
#   r-common::install_pkgs
############################################################

############################################################
# For improved error handling ...
# Add this line to the main() function:
#   strict_mode
# Add these lines to the end of the script before the call to main
#   #trap error_message ERR
#   trap 'excode=$?; error_message; trap - EXIT; echo $excode' EXIT HUP INT QUIT PIPE TERM
#   function error_message {
#     local -r install_packages_out=".install.packages.out"
#       >&2 echo "Error installing R packages, see ${install_packages_out} for details";
#       exit $?
#   }
#
# Note: this needs additional testing before it's ready 
#   for production. Must double-check that Rscript errors
#   are handled differently than r package installer errors
############################################################

main() {
  local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${__dir}/gp-common.sh"
  source "${__dir}/r-common.sh"
  
  # '--dry-run', shortcut for '-e' 'GP_DRY_RUN=true'
  if [[ "$1" = "--dry-run" ]]; then
    export GP_DRY_RUN="true"
    shift;
  fi
  
  # '--debug', shortcut for '-e' 'GP_DEBUG=true'
  if [[ "$1" = "--debug" ]]; then
    export GP_DEBUG="true"
    shift;
  fi
  
  # '-v' r-version, shortcut for '-e' 'GP_R_VERSION=r-version'
  local r_version="";
  if [[ "$1" = "-v" ]]; then
    shift;
    r_version="$1"
    shift;
    export GP_R_VERSION="$r_version"
  fi
  
  parse_args "${@}"
  addEnv "r/${r_version}"
  init_module_envs
  export_r_env
  # by default, GP_MKDIRS=true
  [[ -z "${GP_MKDIRS+x}" ]] \
    && GP_MKDIRS="TRUE";
  
  if is_debug; then
    log "$(env | sort)"
  fi

  if [[ -e "${GP_R_PACKAGE_INFO:-}" ]]; then
    #log "installing R packages ..."
    #log "     GP_R_PACKAGE_INFO='${GP_R_PACKAGE_INFO}'"
    install_pkgs
    #log "installing R packages ... Done!"    
  fi

  # run the Rscript command
  $DRY_RUN Rscript "${__gp_module_cmd[@]}"
}

main "$@"
