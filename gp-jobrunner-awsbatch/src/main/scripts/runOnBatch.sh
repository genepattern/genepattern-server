#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

# Is $1 an integer
function is_int() {
  if [ $# -eq 0 ]; then return 1; fi 
  local arg;
  printf -v arg '%d\n' "${1:-x}" 2>/dev/null;
}

# Is $1 an integer, optionally in the given range
# Usage:
#   in_range arg [min] [max] 
# Return true if arg is an integer >= min and <= max
#
function in_range() { 
  # arg1 must be an integer
  [ $# -ge 1 ] && is_int "${1}" || return 1;
  local arg=$1;

  # optional min range, arg2
  #   note: '-z string', true if the length of string is zero.
  if ! [ -z ${2:+x} ]; then
    # warn: invalid arg, $2 is not an integer
    if ! is_int $2; then return 1; fi
    if (( $arg < $2 )); then return 1; fi
  fi

  # optional max range, arg3
  if ! [ -z ${3:+x} ]; then
    # warn: invalid arg, $3 is not an integer
    if ! is_int $3; then return 1; fi
    if (( $arg > $3 )); then return 1; fi
  fi
}

#
# positional parameters from the JobRunner implementation
#

TASKLIB=$1
WORKING_DIR=$2
JOB_DEFINITION_NAME=$3
JOB_ID=$4
INPUT_FILE_DIRECTORY=$5

shift
shift
shift 
shift
shift

: ${S3_ROOT?not set}
: ${JOB_QUEUE?not set}

# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# create 'exec.sh' script in the GP_METADATA_DIR
: ${GP_METADATA_DIR=$WORKING_DIR/.gp_metadata}
mkdir -p "${GP_METADATA_DIR}"

: ${JOB_STDOUT=$GP_METADATA_DIR/stdout.txt}
: ${JOB_STDERR=$GP_METADATA_DIR/stderr.txt}

EXEC_SHELL="${GP_METADATA_DIR}/exec.sh"

S3_LOG=${GP_METADATA_DIR}/s3_uploads.log
CMD_LOG=${GP_METADATA_DIR}/aws_cmd.log

echo "#!/usr/bin/env bash" > $EXEC_SHELL
echo "" >> $EXEC_SHELL

# job.walltime, kill the job after GP_JOB_WALLTIME_SEC seconds ...
#   default walltime, 4 hours, 14400 hours
#   max walltime, 1 week, 604800 seconds
# 
# Implement job timeout with a perl one-liner which "execs" the 
#   module command line after setting up the alarm
# See: http://mywiki.wooledge.org/BashFAQ/068
#
echo "calculating job.walltime limit ..." >> ${CMD_LOG} 2>&1
echo "    GP_JOB_WALLTIME_SEC=${GP_JOB_WALLTIME_SEC:-x}" >> ${CMD_LOG} 2>&1
: ${GP_JOB_WALLTIME_SEC=14400}
if in_range "${GP_JOB_WALLTIME_SEC:-x}" "1" "604800"; then
  # no-op
  :
else
  GP_JOB_WALLTIME_SEC=14400;
fi
echo "    GP_JOB_WALLTIME_SEC=${GP_JOB_WALLTIME_SEC:-x}" >> ${CMD_LOG} 2>&1

echo "# wrapper script, cancel the task  ..." >> $EXEC_SHELL
echo "\
doalarm() { 
  local timeout_sec=\$1;
  perl -e '\
my \$timeout_sec=shift; \
\$SIG{ALRM} = sub { \
  print STDERR \"Job timed out after \$timeout_sec seconds\n\"; \
  exit(142); \
}; \
open STDOUT, \">\", \"${JOB_STDOUT}\"; \
open STDERR, \">\", \"${JOB_STDERR}\"; \
alarm \$timeout_sec; \
my \$system_code=system @ARGV; \
alarm 0; \
my \$exit_code=\$system_code >> 8; \
exit \$exit_code' \
-- \"\${@}\";
  
}
" >> $EXEC_SHELL

# copy data files from s3 into the container
echo "# sync from s3 into the container" >> $EXEC_SHELL
echo "cd ${GP_METADATA_DIR}" >> $EXEC_SHELL
echo "sh aws-sync-from-s3.sh" >> $EXEC_SHELL

echo "" >> $EXEC_SHELL
echo "cd ${WORKING_DIR}" >> $EXEC_SHELL

printf "doalarm ${GP_JOB_WALLTIME_SEC} " >> $EXEC_SHELL
for arg in "$@"
do
  printf %q "${arg}" >> $EXEC_SHELL
  printf " " >> $EXEC_SHELL
done

# optionally, redirect stdin from a file
if [[ -s $GP_STDIN_FILE ]]; then
  printf %s " < " >> $EXEC_SHELL
  printf %q "${GP_STDIN_FILE}" >> $EXEC_SHELL
fi

echo "" >> $EXEC_SHELL

chmod u+x $EXEC_SHELL

REMOTE_COMMAND=$EXEC_SHELL

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORY $S3_ROOT$INPUT_FILE_DIRECTORY >> ${S3_LOG} 2>&1
aws s3 sync $TASKLIB              $S3_ROOT$TASKLIB              >> ${S3_LOG} 2>&1
aws s3 sync $WORKING_DIR          $S3_ROOT$WORKING_DIR          >> ${S3_LOG} 2>&1
aws s3 sync $GP_METADATA_DIR      $S3_ROOT$GP_METADATA_DIR      >> ${S3_LOG} 2>&1

#
# initialize 'aws batch submit-job' args ...
#

# memory override, e.g.
#   --container-overrides memory=2000
# min, 400 MiB
# max, 1 TiB, or 1024 GiB, or 1024x1024 MiB or 953674 MiB
echo "calculating --container-overrides for memory ..." >> ${CMD_LOG} 2>&1
echo "    GP_JOB_MEMORY_MB=${GP_JOB_MEMORY_MB:-x}" >> ${CMD_LOG} 2>&1
mem_arg="";
if in_range "${GP_JOB_MEMORY_MB:-x}" "400" "1000000"; then
  mem_arg="memory=${GP_JOB_MEMORY_MB},";
fi

# vcpus override, e.g. 
#   --container-overrides vcpus=integer
# min, 1 vcpu
# max, 256 vcpu
echo "calculating --container-overrides for vcpus ..." >> ${CMD_LOG} 2>&1
echo "    GP_JOB_CPU_COUNT=${GP_JOB_CPU_COUNT:-x}" >> ${CMD_LOG} 2>&1
vcpus_arg="";
if in_range "${GP_JOB_CPU_COUNT:-x}" "1" "256"; then
  vcpus_arg="vcpus=${GP_JOB_CPU_COUNT},";
fi

# environment override
__env_arg="environment=[{name=GP_METADATA_DIR,value=${GP_METADATA_DIR}}, \
  {name=STDOUT_FILENAME,value=${GP_METADATA_DIR}/stdout.txt}, \
  {name=STDERR_FILENAME,value=${GP_METADATA_DIR}/stderr.txt} \
]";

__args=( \
  "--job-name" "$JOB_ID" \
  "--job-queue" "$JOB_QUEUE" \
  "--job-definition" "$JOB_DEFINITION_NAME" \
  "--parameters" "taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1=$REMOTE_COMMAND"  \
  "--container-overrides" "${vcpus_arg}${mem_arg}${__env_arg:-}" \
);

# for debugging ...
echo   "AWS_PROFILE=${AWS_PROFILE:- (not set)}" >> ${CMD_LOG}
echo   "    S3_ROOT=${S3_ROOT:- (not set)}" >> ${CMD_LOG}
echo   "  JOB_QUEUE=${JOB_QUEUE:- (not set)}" >> ${CMD_LOG}
echo   "aws batch submit-job" >> ${CMD_LOG}
printf "  '%s'\n" "${__args[@]}" >> ${CMD_LOG}
echo >> ${CMD_LOG}

# run 'aws batch submit-job' command ...
aws batch submit-job "${__args[@]}" | \
      python -c "import sys, json; print( json.load(sys.stdin)['jobId'])"
