#!/usr/bin/env bash

#
# Wrapper script for running an Rscript command
# Calls run-with-env.
#
# Usage: run-script -c <env.custom> -v <R.version>
#     -p <patches>, R_LIBS_SITE=<patches>/Library/R/<R.version> 
#     [-d (TRUE|FALSE)], debug mode 
#     [-m (TRUE|FALSE)], mkdirs 
#     <args>, Rscript command line args
#

_gp_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
# add this directory to the path
#export PATH="${_gp_script_dir}:${PATH}"

#################################
# parse args  (getopts)
#################################
gp_mkdirs="TRUE";
gp_debug="FALSE";
idx=0
while getopts c:v:p:d:m: opt "$@"; do
    case $opt in
        c)  env_custom="$OPTARG"
            ;;
        v) r_version="$OPTARG"
            ;;
        p) gp_patches="$OPTARG"
            ;;
        d)
            gp_debug="$OPTARG"
            ;;
        m)
            gp_mkdirs="$OPTARG"
            ;; 
        *)
            # Unexpected option, exit with status of last command
            exit $?
            ;;
    esac
    idx=$((idx+1))
    # need this line to remove the option from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done

#
# optionally clean up options delimiter '--' 
#
if [ "$1" = "--" ]; then
    shift;
fi

#
# build an array of args
#
MY_ARGS=( "${_gp_script_dir}/run-with-env.sh" \
    -c "${env_custom}" \
    -u "R-${r_version}" \
    -e "GP_DEBUG=${gp_debug}" \
    -e "GP_MKDIR_R_LIBS_SITE=${gp_mkdirs}" \
    -e "R_LIBS=" \
    -e "R_LIBS_USER=' '" \
    -e "R_LIBS_SITE=${gp_patches}/Library/R/${r_version}" \
    -e "R_ENVIRON=${_gp_script_dir}/R/Renviron.gp.site" \
    -e "R_ENVIRON_USER=${_gp_script_dir}/R/${r_version}/Renviron.gp.site" \
    -e "R_PROFILE=${_gp_script_dir}/R/${r_version}/Rprofile.gp.site" \
    -e "R_PROFILE_USER=${_gp_script_dir}/R/${r_version}/Rprofile.gp.custom" \
    "Rscript" \
    "$@" );

#
# exec
#
exec "${MY_ARGS[@]}"
