#!/usr/bin/env bash

############################################################
# run-with-docker.sh (v0.2)
#   Wrapper script for running a GenePattern module in a local
# docker container
#
# Usage:
#   run-with-docker.sh [run-with-env-args]
#
# Configuration 
#   Copy this file to your ./resources/wrapper_scripts directory
#   Edit your ./resources/config_custom.yaml file
#
# default.properties:
#     ...
#     # Basic
#     run-with-docker: "<wrapper-scripts>/run-with-docker.sh -c <env-custom> -e GP_JOB_DOCKER_IMAGE=<job.docker.image> -u docker"
#
#     # Advanced (with additional customization)
#     "docker": "/usr/local/bin/docker"
#     "job.docker.bind_src": "/Users"
#     "job.docker.bind_dst": "/Users"
#     "job.workingDir": "<jobs>/<job_id>"
#     run-with-docker: "<wrapper-scripts>/run-with-docker.sh -c <env-custom> \
#         -e GP_DRY_RUN=<job.env.GP_DRY_RUN> \
#         -e DOCKER_CMD=<docker> \
#         -e GP_JOB_WORKING_DIR=<job.workingDir> \
#         -e DOCKER_BIND_SRC=<job.docker.bind_src> \
#         -e DOCKER_BIND_DST=<job.docker.bind_dst> \
#         -e GP_JOB_DOCKER_IMAGE=<job.docker.image> \
#         -u docker"
#
#     python_3.6: "<run-with-docker> python3"
#
#     ...
#
# module.properties:
#     ConvertLineEndings:
#         job.docker.image: "genepattern/docker-perl52:0.1"
#         perl: "<run-with-docker> perl"
#
#     "txt2odf":
#         job.docker.image: "genepattern/docker-python36:0.4"
#         "python_3.6": "<run-with-docker> python3"
#
############################################################

# Run the command in a docker container ...
#    see: https://docs.docker.com/engine/reference/commandline/run/
#    see: https://docs.docker.com/storage/bind-mounts/
#  Template:
#    # (option 1: with '--mount')
#    docker run -w {dir} --mount type=bind,src={bind_src},dst={bind_dst} {image} {args}
#    # (option 2: with '--volume')
#    docker run -w {dir} -v {bind_src}:{bind_dst} {image} {args}
#  Example:
#    docker run -w "`pwd`" --mount type=bind,src=/Users,dst=/Users genepattern/docker-python36:0.4 python3 --version
#    docker run -w "`pwd`" -v "/Users:/Users" genepattern/docker-python36:0.4 python3 --version


### copy of gp-common.sh:run-with-env()
function run_with_docker() {
  local script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${script_dir}/gp-common.sh"
  parse_args "${@}";
  init_module_envs;
  
  ## check for required arg
  #    note: -z $var-name will fail when var-name is not set AND when var-name is the empty string
  #    note: see http://tldp.org/LDP/abs/html/exitcodes.html#EXITCODESREF
  if [ -z "${GP_JOB_DOCKER_IMAGE}" ]; then
    echo "Missing required variable: 'job.docker.image'" >&2
    exit 64;
  fi
  
  local docker_cmd="${DOCKER_CMD:-/usr/local/bin/docker}";
  local bind_src="${DOCKER_BIND_SRC:-/Users}";
  local bind_dst="${DOCKER_BIND_DST:-/Users}";
  local docker_img="${GP_JOB_DOCKER_IMAGE:-genepattern/docker-java17:0.12}";
  local workdir="${GP_JOB_WORKING_DIR:-`pwd`}";

  # with '--mount'
  $DRY_RUN "${docker_cmd}" run \
    --workdir "${workdir}" \
    --mount "type=bind,src=${bind_src},dst=${bind_dst}" \
    "${docker_img}" \
    "${__gp_module_cmd[@]}";
}

run_with_docker "${@}"
