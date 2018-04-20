#!/usr/bin/env bash

############################################################
# run-with-docker.sh (v0.1)
#   Wrapper script for running a GenePattern module in a local
# docker container
#
# Usage:
#   run-with-docker.sh [run-with-env-args]
#
# Configuration 
#   Example config_custom.yaml file for a local Mac OS X GP server
#
# default.properties:
#     ...
#     run-with-docker: "<wrapper-scripts>/run-with-docker.sh -c <env-custom> -e GP_JOB_DOCKER_IMAGE=<job.docker.image> -u docker"
#     ...
#
# module.properties:
#     ConvertLineEndings:
#         job.docker.image: "genepattern/docker-perl52:0.1"
#         perl: "<run-with-docker> perl"
#
#     "txt2odf":
#         job.docker.image: "genepattern/docker-python36:0.4"
#         "python_3.6": "<run-with-docker> python"
#
############################################################

#
# Example docker command line ...
#   $ docker run -w `pwd` -v "/Users:/Users" genepattern/docker-perl52:0.1 perl -version
# 

### copy of gp-common.sh:run-with-env()
function run_with_docker() {
  local __dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${__dir}/gp-common.sh"
  parse_args "${@}";
  init_module_envs;
  
  ## check for required arg
  #    note: -z $var-name will fail when var-name is not set AND when var-name is the empty string
  #    note: see http://tldp.org/LDP/abs/html/exitcodes.html#EXITCODESREF
  if [ -z "${GP_JOB_DOCKER_IMAGE}" ]; then
    echo "Missing required variable: 'job.docker.image'" >&2
    exit 64;
  fi
  
  ## run the command in a docker container ...
  #    see: https://docs.docker.com/engine/reference/commandline/run/
  $DRY_RUN /usr/local/bin/docker run -w "`pwd`" -v "/Users:/Users" "${GP_JOB_DOCKER_IMAGE}" "${__gp_module_cmd[@]}";
}

run_with_docker "${@}"
