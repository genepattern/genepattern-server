#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch

TASKLIB=$TEST_ROOT/R313_cli/tests/affy/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/R313_cli/tests/affy/data
JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_R313_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/R313_cli/tests/affy/job_12345

COMMAND_LINE="Rscript --no-save --quiet --slave --no-restore $TASKLIB/run_gp_affyst_efc.R $TASKLIB --input.file=$INPUT_FILE_DIRECTORIES/inputFileList.txt --normalize=yes --background.correct=no --qc.plot.format=skip --annotate.rows=yes --output.file.base=tedsOutputFile"



docker run -m 5g -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r313_cli $COMMAND_LINE


