#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TASKLIB=/Users/liefeld/dockerProjects/modules/cms/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/dockerProjects/modules/cms/data
JOB_DEFINITION_NAME="ComparativeMarkerSelection:4"
JOB_ID=gp_job_R25_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/dockerProjects/modules/cms/job_12345

COMMAND_LINE="java -Dlibdir\=./ -cp $TASKLIB/gp-modules.jar:$TASKLIB/commons-math-1.2.jar:$TASKLIB/trove.jar:$TASKLIB/Jama-1.0.2.jar:$TASKLIB/colt.jar:$TASKLIB/jsci-core.jar org.broadinstitute.marker.MarkerSelection $INPUT_FILE_DIRECTORIES/all_aml_train.gct $INPUT_FILE_DIRECTORIES/all_aml_train.cls 100 2 cms_outfile false false no 1 12345  false false -lfalse"

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern

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

