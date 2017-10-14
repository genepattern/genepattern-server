#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

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
#: ${GP_JOB_MEMORY?not set}

# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# create 'exec.sh' script in the GP_METADATA_DIR
: ${GP_METADATA_DIR=$WORKING_DIR/.gp_metadata}
mkdir -p "${GP_METADATA_DIR}"

EXEC_SHELL="${GP_METADATA_DIR}/exec.sh"

echo "#!/usr/bin/env bash" > $EXEC_SHELL
echo "" >> $EXEC_SHELL

# new stuff, dynamic aws s3 sync into the container
echo "# sync from s3 into the container" >> $EXEC_SHELL
echo "cd ${GP_METADATA_DIR}" >> $EXEC_SHELL
echo "sh aws-sync-from-s3.sh" >> $EXEC_SHELL

echo "" >> $EXEC_SHELL
echo "cd ${WORKING_DIR}" >> $EXEC_SHELL

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

S3_LOG=${GP_METADATA_DIR}/s3_uploads.log
CMD_LOG=${GP_METADATA_DIR}/aws_cmd.log

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORY $S3_ROOT$INPUT_FILE_DIRECTORY >> ${S3_LOG} 2>&1
aws s3 sync $TASKLIB              $S3_ROOT$TASKLIB              >> ${S3_LOG} 2>&1
aws s3 sync $WORKING_DIR          $S3_ROOT$WORKING_DIR          >> ${S3_LOG} 2>&1
aws s3 sync $GP_METADATA_DIR      $S3_ROOT$GP_METADATA_DIR      >> ${S3_LOG} 2>&1

#       --container-overrides memory=2000 \

# initialize 'aws batch submit-job' args ...
__env_arg="environment=[{name=GP_METADATA_DIR,value=${GP_METADATA_DIR}}, \
  {name=STDOUT_FILENAME,value=${GP_METADATA_DIR}/stdout.txt}, \
  {name=STDERR_FILENAME,value=${GP_METADATA_DIR}/stderr.txt} \
]";

__args=( \
  "--job-name" "$JOB_ID" \
  "--job-queue" "$JOB_QUEUE" \
  "--job-definition" "$JOB_DEFINITION_NAME" \
  "--parameters" "taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1=$REMOTE_COMMAND"  \
  "--container-overrides" "${__env_arg:-}" \
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
