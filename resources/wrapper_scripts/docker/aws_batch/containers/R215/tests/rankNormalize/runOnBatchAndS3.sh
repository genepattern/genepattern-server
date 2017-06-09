#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#

TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/R215/tests/rankNormalize
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111
RLIB=$TEST_ROOT/rlib


# run_rank_normalize.R, /xchip/gpprod/servers/genepattern/taskLib/RankNormalize.1.9.17547/, --input.file=/xchip/gpprod/servers/genepattern/users/ted/uploads/tmp/run6841.file/1/all_aml_train.gct, --output.file.name=all_aml_train.NORM.gct
 
COMMAND_LINE="Rscript $TASKLIB/run_rank_normalize.R $TASKLIB --input.file=$INPUT_FILE_DIRECTORIES/all_aml_train.gct --output.file.name=all_aml_train.NORM.gct"

JOB_DEFINITION_NAME="R215_Generic"
JOB_ID=gp_job_REVEALER_R215_$1
JOB_QUEUE=TedTest


#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern

#       --container-overrides memory=2000 \

aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --container-overrides 'memory=3600' \
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

