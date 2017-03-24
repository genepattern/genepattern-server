#!/bin/bash

.  ~/.bash_profile
. /Users/liefeld/.genepattern/resources/wrapper_scripts/docker/aws_batch/scripts/gpAwsBatchConf.sh


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

COMMAND_LINE=$@

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORY $S3_ROOT$INPUT_FILE_DIRECTORY --profile genepattern >> .s3_uploads.stdout 2>&1
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern >> .s3_uploads.stdout 2>&1

#       --container-overrides memory=2000 \


echo aws batch submit-job  --job-name $JOB_ID  --job-queue $JOB_QUEUE  --job-definition $JOB_DEFINITION_NAME   --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$COMMAND_LINE"  --profile genepattern >> .s3_uploads.stdout



aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --job-definition $JOB_DEFINITION_NAME \
      --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$COMMAND_LINE"  \
      --profile genepattern | python -c "import sys, json; print( json.load(sys.stdin)['jobId'])"

# may want to pipe the submit output through this to extract the job ID for checking status
# | python -c "import sys, json; print json.load(sys.stdin)['jobId']"


