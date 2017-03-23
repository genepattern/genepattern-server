#!/usr/bin/env bash

#
# Wrapper script for running an Rscript command
# Calls run-with-env.
#
# Usage: run-script -c <env.custom> -v <R.version>
#     -n, dry-run 
#     -l <libdir>, path to installation directory for the module
#     -p <patches>, R_LIBS_SITE=<patches>/Library/<env-arch>/R/<R.version> 
#     [-d (TRUE|FALSE)], debug mode 
#     [-m (TRUE|FALSE)], mkdirs 
#     <args>, Rscript command line args
#

__gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

#################################
# parse args  (getopts)
#################################
gp_mkdirs="TRUE";
gp_debug="FALSE";
DRY_RUN="";
gp_env_arch="";

# optional '-c <env-custom>' flag
if [[ "${1:-}" = "-c" ]]; then
    shift; 
    # only set if '-c' has an argument
    if ! [[ -z "${1+x}" || $1 = -* ]]; then
        env_custom="${1:-}";
        echo "found match for env_custom=${env_custom}";
        shift;
    else
        echo "no match for env_custom, skipping";
    fi
fi

while getopts c:v:l:a:p:d:m:n opt "$@"; do
    case $opt in
        c)  
            #env_custom="$OPTARG"
            echo "ignoring '-c' arg"
            ;;
        v) r_version="$OPTARG"
            ;;
        # libdir ends with '/'
        l) gp_libdir="$OPTARG"
            ;;
        # env-arch does not end with '/', can be empty
        a) gp_env_arch="${OPTARG}/"
            ;;
        # patches does not end with '/'
        p) gp_patches="$OPTARG"
            ;;
        d)
            gp_debug="$OPTARG"
            ;;
        m)
            gp_mkdirs="$OPTARG"
            ;; 
        n)  DRY_RUN="echo"
            ;;
        *)
            # Unexpected option, exit with status of last command
            exit $?
            ;;
    esac
    # need this line to remove the option from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done

#
# optionally clean up options delimiter '--' 
#
if [ "$1" = "--" ]; then
    shift;
fi

_gp_r_libs_site="${gp_patches}/${gp_env_arch}Library/R/${r_version}"

#
# Rscript wrapper command
#
declare -a c_opts=( "-c" "${env_custom}" );
if [[ -z "${env_custom:-}" ]]; then
    c_opts=();
else
    c_opts=('-c' "${env_custom}");
fi

RSCRIPT_CMD=( "${__gp_script_dir}/run-with-env.sh" \
    "${c_opts[@]}" \
    -u "R-${r_version}" \
    -e "GP_DEBUG=${gp_debug}" \
    -e "R_LIBS=" \
    -e "R_LIBS_USER=' '" \
    -e "R_LIBS_SITE=${_gp_r_libs_site}" \
    -e "R_ENVIRON=${__gp_script_dir}/R/Renviron.gp.site" \
    -e "R_ENVIRON_USER=${__gp_script_dir}/R/${r_version}/Renviron.gp.site" \
    -e "R_PROFILE=${__gp_script_dir}/R/${r_version}/Rprofile.gp.site" \
    -e "R_PROFILE_USER=${__gp_script_dir}/R/${r_version}/Rprofile.gp.custom" \
    "Rscript" );

#
# optionally validate/install R packages
#

# install packages report log file
install_packages_log=".install.packages.log"
# redirect stderr/stdout to this file
install_packages_out=".install.packages.out"

if [ -e "$gp_libdir/r.package.info" ]; then
    # mkdirs
    if [ "$gp_mkdirs" = "TRUE" ]; then
        $DRY_RUN "mkdir" "-p" "${_gp_r_libs_site}"
    fi

    # --no-save --quiet --slave --no-restore <gp.tools.dir>/R/install_packages/installPackages.R
    INSTALL_PACKAGES_CMD=( "${RSCRIPT_CMD[@]}" \
        "--no-save" "--quiet" "--slave" "--no-restore" \
        "${_gp_script_dir}/R/installPackages.R" \
        "${gp_libdir}r.package.info" \
        "${install_packages_log}"
    );

    $DRY_RUN "${INSTALL_PACKAGES_CMD[@]}" >>"${install_packages_out}" 2>&1
    
    # check exit code
    if [ $? -ne 0 ]; then
        >&2 echo "Error installing R packages, see ${install_packages_out} for details";
        exit $?
    fi
fi


#
# run the module Rscript command
#
MODULE_CMD=( "${RSCRIPT_CMD[@]}" "$@" );
$DRY_RUN "${MODULE_CMD[@]}"
