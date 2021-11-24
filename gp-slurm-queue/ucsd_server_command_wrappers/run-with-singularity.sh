#!/usr/bin/env bash
############################################################
# run-with-singularity.sh (v0.1)
#   Wrapper script for running a GenePattern module in a local
# singularity container
#
# Usage:
#   run-with-singularity.sh [run-with-env-args]
#
# Configuration 
#   Copy this file to your ./resources/wrapper_scripts directory
#   Edit your ./resources/config_custom.yaml file
#
# default.properties:
#     ...
#     # Basic
#     run-with-singularity: "<wrapper-scripts>/run-with-singularity.sh -c <env-custom> -e GP_JOB_DOCKER_IMAGE=<job.docker.image> -u singularity"
#
#     # Advanced (with additional customization)
#     "singularity": "/usr/local/bin/singularity"
#     "job.singularity.bind_src": "/Users"
#     "job.singularity.bind_dst": "/Users"
#     "job.workingDir": "<jobs>/<job_id>"
#     run-with-singularity: "<wrapper-scripts>/run-with-singularity.sh -c <env-custom> \
#         -e GP_DRY_RUN=<job.env.GP_DRY_RUN> \
#         -e SINGULARITY_CMD=<docker> \
#         -e GP_JOB_WORKING_DIR=<job.workingDir> \
#         -e SINGULARITY_BIND_SRC=<job.docker.bind_src> \
#         -e SINGULARITY_BIND_DST=<job.docker.bind_dst> \
#         -e GP_JOB_DOCKER_IMAGE=<job.docker.image> \
#         -u singularity"
#
#     python_3.6: "<run-with-docker> python3"
#
#     ...
#
# module.properties:
#     ConvertLineEndings:
#         job.docker.image: "genepattern/docker-perl52:0.1"
#         perl: "<run-with-singularity> perl"
#
#     "txt2odf":
#         job.docker.image: "genepattern/docker-python36:0.4"
#         "python_3.6": "<run-with-singularity> python3"
#
############################################################

# Run the command in a singularity (3.1) container ...
#    see: https://www.sylabs.io/guides/3.1/user-guide
#  Example:
#    singularity exec -B /Users:/Users:rw docker://genepattern/docker-python36:0.4 python3 --version

#
# override the cache to put it on a spacious disk.  We will save sif images here and also put a tmp dir
# under this cache
#
export SINGULARITY_CACHEDIR=/expanse/projects/mesirovlab/genepattern/singularity_cache

### copy of gp-common.sh:run-with-env()
function run_with_singularity() {
  local script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  source "${script_dir}/gp-common.sh"
  parse_args "${@}";
  init_module_envs;
  
  ## check for required arg, 'job.docker.image'
  #    note: -z $var-name will fail when var-name is not set AND when var-name is the empty string
  #    note: see http://tldp.org/LDP/abs/html/exitcodes.html#EXITCODESREF
  if [ -z "${GP_JOB_DOCKER_IMAGE}" ]; then
    echo "Missing required variable: 'job.docker.image'" >&2
    exit 64;
  fi
  
  local singularity_cmd="${SINGULARITY_CMD:-/usr/local/bin/singularity}";
  local bind_src="/expanse/projects/mesirovlab/genepattern/servers/ucsd.prod";
  local bind_dst="/expanse/projects/mesirovlab/genepattern/servers/ucsd.prod";
  # note: this default is ignored by the check for 'job.docker.image' above
  local docker_img="${GP_JOB_DOCKER_IMAGE:-genepattern/docker-java17:0.12}";

  # unlike docker, Singularity runs in the PWD its called from by default so we don't set the workdir when running
  local workdir="${GP_JOB_WORKING_DIR:-`pwd`}";
 
  ## check for 'docker' executable
  #    use the 'command -v' idiom to check the docker_cmd
  #    it will have a non-zero status if the command is not found
  # Example:
  #   $ command -v /usr/local/bin/docker
  #   $ echo $?
  # See:
  #   http://pubs.opengroup.org/onlinepubs/9699919799/utilities/command.html
  #command -v "$singularity_cmd"
  #echo SING CHECK $?
  #echo hostname $HOSTNAME 
  #echo "$singularity_cmd"
  #echo ls -alrt /usr/local/bin
  #env

  command -v "$singularity_cmd" >/dev/null || { 
    echo "\
Command not found: '${singularity_cmd}'

Make sure to install Singularity and configure your GenePattern Server.

To install Singularity  on Mac OS X ...
  See: https://singularity.lbl.gov/install-mac
  
To configure your server ...
  Set the 'singularity' property in your config_yaml file. E.g.
default.properties:
    ...
    # path to singularity executable
    \"singularity\": \"/usr/local/bin/singularity\"
" >&2; exit 127; 
  }

  # echo "${singularity_cmd}" exec  -B ${bind_src}:${bind_dst}:rw  -W ${GP_JOB_WORKING_DIR} "docker://${docker_img}" "${__gp_module_cmd[@]}" > ${workdir}/sing_exec.txt

  #export SINGULARITY_CACHEDIR=/expanse/projects/mesirovlab/genepattern/singularity_cache
  export SINGULARITY_TMPDIR=${SINGULARITY_CACHEDIR}/tmp
  export GP_SINGULARITY_IMAGEDIR=${SINGULARITY_CACHEDIR}/images

  ### singularity pulls fail of the image is in the dir already, but we don't want to put it in the job dir because they are big.  To check the 
  ### cache for an image we must do the docker-singularity sif filename conversion ourselves which might cause errors
  # singularity pull "docker://${docker_img}"
  # singularity pull --dir ${GP_SINGULARITY_IMAGEDIR}  "docker://${docker_img}" 


  cd ${workdir} 
  "${singularity_cmd}" exec  -B ${bind_src}:${bind_dst}:rw  -W ${GP_JOB_WORKING_DIR} "docker://${docker_img}" "${__gp_module_cmd[@]}" 

}

run_with_singularity "${@}"
