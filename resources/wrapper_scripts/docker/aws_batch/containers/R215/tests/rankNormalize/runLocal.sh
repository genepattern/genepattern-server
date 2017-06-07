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


docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR -v $RLIB:/build/rlib -w $WORKING_DIR -e LIBDIR=$TASKLIB -e R_LIBS=/build/rlib -e TASKLIB=$TASKLIB  liefeld/r215 runLocal.sh $COMMAND_LINE


