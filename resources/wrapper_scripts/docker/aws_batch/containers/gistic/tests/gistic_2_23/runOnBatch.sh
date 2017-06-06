#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/gistic/tests/gistic_2_23
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
JOB_DEFINITION_NAME="Gistic_2_0_23"
JOB_ID=gp_job_gistic_2023_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_12345


#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern

#       --container-overrides memory=2000 \

COMMAND_LINE="$TASKLIB/gp_gistic2_from_seg -b . -seg $INPUT_FILE_DIRECTORIES/segmentationfile.txt -mk $INPUT_FILE_DIRECTORIES/markersfile.txt -cnv $INPUT_FILE_DIRECTORIES/cnvfile.txt -refgene $INPUT_FILE_DIRECTORIES/Human_Hg19.mat -td 0.1 -js 4 -qvt 0.25 -rx 1 -cap 1.5 -conf 0.90 -genegistic 1 -broad 0 -brien 0.50 -maxseg 2500 -ampeel 0 -scent median -gcm extreme -arb 1 -peak_type robust -fname segmentationfile -genepattern 1 -twosides 0 -saveseg 0 -savedata 0 -smalldisk 0 -smallmem 1 -savegene 1 -v 0 "


aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --container-overrides memory=4600 \
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

