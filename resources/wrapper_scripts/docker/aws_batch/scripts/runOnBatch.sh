#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"
source "${script_dir}/gpAwsBatchConf.sh"

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
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
# Make the input file directory since we need to put the script to execute in it
mkdir -p $WORKING_DIR/.gp_metadata

EXEC_SHELL=$WORKING_DIR/.gp_metadata/exec.sh

echo "#!/usr/bin/env bash" > $EXEC_SHELL
echo "" >> $EXEC_SHELL
echo $@ >> $EXEC_SHELL

#chmod a+rwx $EXEC_SHELL
#chmod -R a+rwx $WORKING_DIR
chmod u+x $EXEC_SHELL


REMOTE_COMMAND="runS3OnBatch.sh $TASKLIB $INPUT_FILE_DIRECTORY $S3_ROOT $WORKING_DIR $EXEC_SHELL"
# note the batch submit now uses REMOTE_COMMAND instead of COMMAND_LINE 

#
# Copy the input files to S3 using the same path
#
#aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1
#aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern
#aws s3 sync $WORKING_DIR $S3_ROOT$WORKING_DIR --profile genepattern 

######### end new part for script #################################################

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORY $S3_ROOT$INPUT_FILE_DIRECTORY $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1
aws s3 sync $WORKING_DIR $S3_ROOT$WORKING_DIR $AWS_PROFILE_ARG >> .s3_uploads.stdout 2>&1

#       --container-overrides memory=2000 \


#echo aws batch submit-job  --job-name $JOB_ID  --job-queue $JOB_QUEUE  --job-definition $JOB_DEFINITION_NAME   --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$REMOTE_COMMAND"  $AWS_PROFILE_ARG >> .s3_uploads.stdout

echo "aws batch submit-job --job-name $JOB_ID --job-queue $JOB_QUEUE --job-definition $JOB_DEFINITION_NAME --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1=$REMOTE_COMMAND" $AWS_PROFILE_ARG >> .s3_uploads.stdout

aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --job-definition $JOB_DEFINITION_NAME \
      --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$REMOTE_COMMAND"  \
      $AWS_PROFILE_ARG | python -c "import sys, json; print( json.load(sys.stdin)['jobId'])"

# may want to pipe the submit output through this to extract the job ID for checking status
# | python -c "import sys, json; print json.load(sys.stdin)['jobId']"


