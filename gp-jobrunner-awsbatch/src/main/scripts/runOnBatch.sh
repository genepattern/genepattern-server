#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"
source "${script_dir}/gpAwsBatchConf.sh"

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

# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# create 'exec.sh' script in the GP_METADATA_DIR
: ${GP_METADATA_DIR=$WORKING_DIR/.gp_metadata}
mkdir -p "${GP_METADATA_DIR}"

EXEC_SHELL="${GP_METADATA_DIR}/exec.sh"

echo "#!/usr/bin/env bash" > $EXEC_SHELL
echo "" >> $EXEC_SHELL
echo "cd ${WORKING_DIR}" >> $EXEC_SHELL

for arg in "$@"
do
  printf %q "${arg}" >> $EXEC_SHELL
  printf " " >> $EXEC_SHELL
done
echo "" >> $EXEC_SHELL

chmod u+x $EXEC_SHELL

REMOTE_COMMAND=$EXEC_SHELL


#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORY $S3_ROOT$INPUT_FILE_DIRECTORY $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1
aws s3 sync $TASKLIB              $S3_ROOT$TASKLIB              $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1
aws s3 sync $WORKING_DIR          $S3_ROOT$WORKING_DIR          $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1
aws s3 sync $GP_METADATA_DIR      $S3_ROOT$GP_METADATA_DIR      $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1

#       --container-overrides memory=2000 \

echo "aws batch submit-job --job-name $JOB_ID --job-queue $JOB_QUEUE --job-definition $JOB_DEFINITION_NAME --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1=$REMOTE_COMMAND" $AWS_PROFILE_ARG >> .s3_uploads.stdout

aws batch submit-job \
    --job-name $JOB_ID \
    --job-queue $JOB_QUEUE \
    --job-definition $JOB_DEFINITION_NAME \
    --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$REMOTE_COMMAND"  \
    --container-overrides environment=[\{name=GP_METADATA_DIR,value=${GP_METADATA_DIR}\}] \
    $AWS_PROFILE_ARG \
| python -c "import sys, json; print( json.load(sys.stdin)['jobId'])"
