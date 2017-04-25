############################################################
# r-install-pkgs.sh
#   Install R packages for a GenePattern module
# Usage:
#   ./r-install-pkgs 
# Input:
#   r-version
#   r-site-library
#     default=<patches>/[<env-arch>/]Library/R/<r-version>
#   r-package-info-file
#     default=<libdir>r.package.info
#   gp-mkdirs
#     default=true
#
# Example:
#   ../r-install-pkgs.sh \
#     -c env-custom-macos.sh \
#     -u R-3.1 \
#     -e GP_R_VERSION=3.1 \
#     -e GP_DRY_RUN=TRUE \
#     -e GP_MKDIRS=TRUE \
#     -e GP_R_LIBS_SITE=${HOME}/tmp/site-library \
#     -e GP_R_PACKAGE_INFO=affy_efc_v0.14_r.package.info \
#   >> stdout.txt
#
############################################################

############################################################
# Function: run_rscript_cmd
#   Run an 'Rscript' module, installing packages if necessary
############################################################
#run_rscript_cmd() {
  # GP_R_LIBS_SITE, default=<patches>/[<env-arch>/]Library/R/<r-version>
  # GP_R_PACKAGE_INFO, default=<libdir>r.package.info
#}

main() {
  local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${__dir}/gp-common.sh"
  strict_mode
  source "${__dir}/r-common.sh"
  parse_args "${@}"
  initModuleEnvs
  export_r_env
  # by default, GP_MKDIRS=true
  [[ -z "${GP_MKDIRS+x}" ]] \
    && GP_MKDIRS="TRUE";
  
  # debug: env | sort
  if [[ -e "${GP_R_PACKAGE_INFO}" ]]; then
    install_pkgs
  fi
}

main "$@"
