#!/usr/bin/env bash

############################################################
# r-common.sh
#   Common R functions for GenePattern wrapper scripts
############################################################

############################################################
# export_r_env
#   Export R environment variables before running Rscript command
#
# Input:
#   GP_SCRIPT_DIR, full path to the wrapper scripts directory
#   GP_R_VERSION, the version of R, e.g. '2.15'
#   GP_R_LIBS_SITE, optional, custom location for R_LIBS_SITE
#     default=<patches>/[<env-arch>/]Library/R/<r-version>
#   GP_R_LIBS_ROOT, 
#     default=<patches>/[<env-arch>/]Library/R
#   GP_R_PACKAGE_INFO, 
#     default=<libdir>r.package.info
#
# Exports the following variables
#   R_LIBS
#   R_LIBS_USER
#   R_LIBS_SITE
#   R_ENVIRON
#   R_ENVIRON_USER
#   R_PROFILE
############################################################
export_r_env() {
  # if [[ -z "${R_LIBS+x}" ]]; then
  if not_set "R_LIBS"; then
    export R_LIBS='';
  fi
  # if [[ -z "${R_LIBS_USER+x}" ]]; then
  if not_set "R_LIBS_USER"; then
    export R_LIBS_USER=' ';
  fi
  # init R_LIBS_SITE
  if not_set "R_LIBS_SITE"; then
    # debug : echo "R_LIBS_SITE is not set"
    if is_set "GP_R_LIBS_SITE"; then
      # debug : echo "GP_R_LIBS_SITE is set"
      export R_LIBS_SITE="${GP_R_LIBS_SITE}"
    elif is_set "GP_R_LIBS_ROOT" && is_set "GP_R_VERSION"; then
      # debug : echo "GP_R_LIBS_ROOT is set: ${GP_R_LIBS_ROOT}"
      # debug : echo "GP_R_VERSION is set: ${GP_R_VERSION}"
      export R_LIBS_SITE="${GP_R_LIBS_ROOT}/${GP_R_VERSION}"
    fi
  fi
  if not_set "R_ENVIRON"; then
    export R_ENVIRON="${GP_SCRIPT_DIR}/R/Renviron.gp.site"
  fi
  # if R_ENVIRON_USER is not set && GP_R_VERSION is set ...
  if not_set "R_ENVIRON_USER" && is_set "GP_R_VERSION"; then
    export R_ENVIRON_USER="${GP_SCRIPT_DIR}/R/${GP_R_VERSION}/Renviron.gp.site"
  fi
  # if R_PROFILE is not set && GP_R_VERSION is set ...
  if not_set "R_PROFILE" && is_set "GP_R_VERSION"; then
    export R_PROFILE="${GP_SCRIPT_DIR}/R/${GP_R_VERSION}/Rprofile.gp.site"
  fi
  # if R_PROFILE_USER is not set && GP_R_VERSION is set ...
  if not_set "R_PROFILE_USER" && is_set "GP_R_VERSION"; then
    export R_PROFILE_USER="${GP_SCRIPT_DIR}/R/${GP_R_VERSION}/Rprofile.gp.custom"
  fi
}

############################################################
# Function: install_pkgs
#   optionally validate/install R packages
############################################################
install_pkgs() {
  # mkdirs
  if is_true "GP_MKDIRS"; then
    $__gp_dry_run "mkdir" "-p" "${R_LIBS_SITE}"
  fi

#TODO implement optional redirect
  # redirect stderr/stdout to this file
  local -r install_packages_out=".install.packages.out"

  # install packages
  $__gp_dry_run "Rscript" "--no-save" "--quiet" "--slave" "--no-restore" \
    "${GP_SCRIPT_DIR}/R/installPackages.R" \
    "${GP_R_PACKAGE_INFO}" \
    ".install.packages.log" 

  # check exit code
  if [ $? -ne 0 ]; then
    >&2 echo "Error installing R packages, see ${install_packages_out} for details";
    exit $?
  fi
}
