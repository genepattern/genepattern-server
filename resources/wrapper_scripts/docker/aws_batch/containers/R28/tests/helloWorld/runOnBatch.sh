#!/bin/sh

# Rscript src/SurvivalCurve.R SurvivalCurve data/surv.txt -c data/surv.cls surv time censor -fcls.clinical F automatic -lt -lc 1 1 -m 0 1 log 0 T left-bottom

# /Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/survival_curve


TASKLIB=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/job_1111

# in this container it automatically does the java -cp RunR before the command
#
COMMAND_LINE="$TASKLIB/hello.R hello"

JOB_DEFINITION_NAME="R28_Generic"
JOB_ID=gp_job_REVEALER_R28_helloWorld_$1
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


