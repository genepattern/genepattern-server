#!/bin/bash

.  ~/.bash_profile

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#

TASKLIB=$1
WORKING_DIR=$2
INPUT_FILE_DIRECTORY=$3
shift
shift
shift
DASHDASH=$4
COMMAND_LINE=$@

JOB_DEFINITION_NAME="Java17_Oracle_Generic"
JOB_ID=gp_job_AffyST_Java17_SFC_FROM_GP
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest

echo $COMMAND_LINE > lastCmd.txt

#COMMAND_LINE="java -cp $TASKLIB/SelectFeaturesColumns.jar:$TASKLIB/gp-modules.jar org.genepattern.modules.selectfeaturescolumns.SelectFeaturesColumns $INPUT_FILE_DIRECTORY/all_aml_train.gct $WORKING_DIR/testout.gct -t1-2"

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORY $S3_ROOT$INPUT_FILE_DIRECTORY --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern

#       --container-overrides memory=2000 \


aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --job-definition $JOB_DEFINITION_NAME \
      --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORY,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$COMMAND_LINE"  \
      --profile genepattern | python -c "import sys, json; print( json.load(sys.stdin)['jobId'])"

# may want to pipe the submit output through this to extract the job ID for checking status
# | python -c "import sys, json; print json.load(sys.stdin)['jobId']"




# wait for job completion TBD
#
#check status 
# aws batch describe-jobs --jobs 07f93b66-f6c0-47e5-8481-4a04722b7c91 --profile genepattern
#

#
# Copy the output of the job back to our local working dir from S3
#
#aws s3 sync $S3_ROOT$WORKING_DIR $WORKING_DIR --profile genepattern

