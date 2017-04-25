#!/usr/bin/env bash

############################################################
# run-rscript-pkgs.sh
#   Run an 'Rscript' module, installing R packages if necessary
# Usage:
#   ./run-rscript-pkgs.sh \
#     [--dry-run] \
#     -v r-version \
#     -c env-custom \
#     -e GP_R_LIBS_SITE=gp-r-libs-site \
#     -e GP_R_PACKAGE_INFO=gp-r-package-info \
#     -- \
#     rscript-args
# References:
#   gp-common::parse_args
#   r-common::export_r_env
#   r-common::install_pkgs
############################################################

############################################################
# Function: run_rscript_cmd
#   Run an 'Rscript' module, installing packages if necessary
############################################################
#run_rscript_cmd() {
  # GP_ROOT_R_LIBS_SITE, default=<patches>/[<env-arch>/]Library/R
  # GP_R_LIBS_SITE, default=<patches>/[<env-arch>/]Library/R/<r-version>
  # GP_R_PACKAGE_INFO, default=<libdir>r.package.info
#}

main() {
  local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${__dir}/gp-common.sh"
  strict_mode
  source "${__dir}/r-common.sh"
  # debug: echo "args: ${@}"
  
  # '--dry-run', shortcut for '-e' 'GP_DRY_RUN=true'
  if [[ "$1" = "--dry-run" ]]; then
    export GP_DRY_RUN="true"
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
  # debug: echo "r_version=$r_version"
  # debug: echo "args: ${@}"
  
  parse_args "${@}"
  addEnv "r/${r_version}"
  initModuleEnvs
  export_r_env
  # by default, GP_MKDIRS=true
  [[ -z "${GP_MKDIRS+x}" ]] \
    && GP_MKDIRS="TRUE";
  
  # debug: env | sort

  if [[ -e "${GP_R_PACKAGE_INFO:-}" ]]; then
    # debug: echo "installing packages ..." 1>&2 ;
    install_pkgs
  fi

  # run the Rscript command
  $DRY_RUN Rscript "${__gp_module_cmd[@]}"
}

main "$@"
