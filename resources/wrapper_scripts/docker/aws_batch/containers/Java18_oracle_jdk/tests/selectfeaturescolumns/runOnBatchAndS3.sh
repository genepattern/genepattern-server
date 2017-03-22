#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/Java18_oracle_jdk/tests
TASKLIB=$TEST_ROOT/selectfeaturescolumns/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/selectfeaturescolumns/data
JOB_DEFINITION_NAME="Java18_Oracle_Generic"
JOB_ID=gp_job_Java18_Oracle_SFC_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/selectfeaturescolumns/job_12

COMMAND_LINE="java -cp $TASKLIB/SelectFeaturesColumns.jar:$TASKLIB/gp-modules.jar org.genepattern.modules.selectfeaturescolumns.SelectFeaturesColumns $INPUT_FILE_DIRECTORIES/all_aml_train.gct $WORKING_DIR/testout.gct -t1-2"

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern

#       --container-overrides memory=2000 \


aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --job-definition $JOB_DEFINITION_NAME \
      --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORIES,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$COMMAND_LINE"  \
      --profile genepattern

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

