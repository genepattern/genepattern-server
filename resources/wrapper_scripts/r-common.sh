#!/usr/bin/env bash

############################################################
# r-common.sh
#   Common R functions for GenePattern wrapper scripts
#
# Environment variables ...
#   GP_SCRIPT_DIR, full path to the wrapper scripts directory
#   GP_R_VERSION, the version of R, e.g. '2.15'
#
# ... for R site-library customization
#   see: https://stat.ethz.ch/R-manual/R-devel/library/base/html/libPaths.html
#   GP_R_LIBS_SITE_PREFIX, default=<patches>/Library/R
#   GP_R_LIBS_SITE, default=(not set)
#     optional custom location for R_LIBS_SITE
#
# ... for R session customization
#   see: https://stat.ethz.ch/R-manual/R-devel/library/base/html/Startup.html
#   see: https://stat.ethz.ch/R-manual/R-devel/library/base/html/EnvVar.html
#
# ... for R package installer customization
#   GP_R_PACKAGE_INFO, default=<libdir>r.package.info
#   GP_MKDIRS, default=true, when true, mkdirs -p r-libs-site
#   GP_INSTALL_PACKAGES_LOG, default=.install.packages.log
#   GP_INSTALL_PACKAGES_OUT, default=.install.packages.out
#
# The export_env function exports the following variables
#   R_LIBS          default="" (empty string)
#   R_LIBS_USER     default=" " (space char) 
#   R_LIBS_SITE     default=<patches>/Library/R/<r-version>
#   R_ENVIRON       default=<wrapper-scripts>/R/Renviron.gp.site
#   R_ENVIRON_USER  default=<wrapper-scripts>/R/<r-version>/Renviron.gp.site
#   R_PROFILE       default=<wrapper-scripts>/R/<r-version>/Rprofile.gp.site
#   R_PROFILE_USER  default=<wrapper-scripts>/R/<r-version>/Rprofile.gp.custom 
#
############################################################

############################################################
# export_r_env
#   Export R environment variables before running Rscript command
############################################################
export_r_env() {
  log "export_r_env(): initializing R environment ..."
  if not_set "R_LIBS"; then
    export R_LIBS='';
  fi
  if not_set "R_LIBS_USER"; then
    export R_LIBS_USER=' ';
  fi
  # init R_LIBS_SITE
  # GP_R_LIBS_SITE takes precedence over pre-existing R_LIBS_SITE
  if is_set "GP_R_LIBS_SITE"; then
    log "     GP_R_LIBS_SITE is set, setting R_LIBS_SITE='${GP_R_LIBS_SITE}'"
    export R_LIBS_SITE="${GP_R_LIBS_SITE}"
  elif not_set "R_LIBS_SITE"; then
    log "     R_LIBS_SITE is not set"
    if is_set "GP_R_LIBS_SITE_PREFIX" && is_set "GP_R_VERSION"; then
      log "     GP_R_LIBS_SITE_PREFIX is set: ${GP_R_LIBS_SITE_PREFIX}"
      log "     GP_R_VERSION is set: ${GP_R_VERSION}"
      export R_LIBS_SITE="${GP_R_LIBS_SITE_PREFIX}/${GP_R_VERSION}"
    elif is_set "GP_R_VERSION"; then
      log "     GP_R_LIBS_SITE_PREFIX is not set: ${GP_R_LIBS_SITE_PREFIX}"
      log "     GP_R_VERSION is set: ${GP_R_VERSION}"
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
  
  if is_debug; then
    log "export_r_env(): R environment variables ..."
    echo_r_env
    log "export_r_env(): Done!"
  fi
}

############################################################
# Function: install_pkgs
#   optionally validate/install R packages
############################################################
install_pkgs() {
  # double check if 'r.package.info' exists
  if [[ ! -e "${GP_R_PACKAGE_INFO:-}" ]]; then
    log "install_pkgs(): skipping R package installation, no 'r.package.info' file found"
    log "     GP_R_PACKAGE_INFO='${GP_R_PACKAGE_INFO}'"
    return
  fi
    
  # install packages report log file
  local -r install_packages_log="${GP_INSTALL_PACKAGES_LOG:-.install.packages.log}"
  # redirect stderr/stdout to this file
  local -r install_packages_out="${GP_INSTALL_PACKAGES_OUT:-.install.packages.out}"

  # mkdirs
  if is_true "GP_MKDIRS"; then
    $DRY_RUN "mkdir" "-p" "${R_LIBS_SITE}"
  fi

  # install packages
  INSTALL_PACKAGES_CMD=( "Rscript" \
    "--no-save" "--quiet" "--slave" "--no-restore" \
    "${GP_SCRIPT_DIR}/R/installPackages.R" \
    "${GP_R_PACKAGE_INFO}" \
    "${install_packages_log}" \
  );
  
  log "install_pkgs(): installing R packages ..."
  log "     GP_R_PACKAGE_INFO='${GP_R_PACKAGE_INFO}'"
  
  $DRY_RUN "${INSTALL_PACKAGES_CMD[@]}" >>"${install_packages_out}" 2>&1

  log "install_pkgs(): Done!"

  # check exit code
  if [ $? -ne 0 ]; then
    #.install.packages.log
    >&2 echo "Error installing R packages for module";
    >&2 echo "  See ${install_packages_log} for summary";
    >&2 echo "  See ${install_packages_out} for details";
    exit $?
  fi
}

############################################################
# Function: echo_r_env
#   for debugging
############################################################
function echo_r_env() {
  echo "     R_LIBS        : ${R_LIBS:- (not set)}";
  echo "     R_LIBS_USER   : ${R_LIBS_USER:- (not set)}";
  echo "     R_LIBS_SITE   : ${R_LIBS_SITE:- (not set)}";
  echo_dir_check "${R_LIBS_SITE}"
  echo "     R_ENVIRON     : ${R_ENVIRON:- (not set)}";
  echo_file_check "${R_ENVIRON}"
  echo "     R_ENVIRON_USER: ${R_ENVIRON_USER:- (not set)}";
  echo_file_check "${R_ENVIRON_USER}"
  echo "     R_PROFILE     : ${R_PROFILE:- (not set)}";
  echo_file_check "${R_PROFILE}"
  echo "     R_PROFILE_USER: ${R_PROFILE_USER:- (not set)}";
  echo_file_check "${R_PROFILE_USER}"
}

# Function: echo_file_check, for debugging
function echo_file_check() {
  local -r file="${1}";
  local -r pad="${2:-                   }"
  if [ ! -e "${file}" ]; then
    echo "${pad}   ('-e', does not exist)"
  elif [ -f "${dir}" ]; then
    echo "${pad}   ('-f', not a regular file)"
  fi
}

# Function: echo_dir_check, for debugging
function echo_dir_check() {
  local -r dir="${1}";
  local -r pad="${2:-                   }"
  if [ ! -e "${dir}" ]; then
    echo "${pad}   ('-e', does not exist)"
  elif [ ! -d "${dir}" ]; then
    echo "${pad}   ('-d', not a directory)"
  fi
}
