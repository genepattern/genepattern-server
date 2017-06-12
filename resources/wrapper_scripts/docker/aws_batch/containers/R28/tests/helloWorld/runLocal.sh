#!/bin/sh

# Rscript src/SurvivalCurve.R SurvivalCurve data/surv.txt -c data/surv.cls surv time censor -fcls.clinical F automatic -lt -lc 1 1 -m 0 1 log 0 T left-bottom

# /Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/survival_curve


TASKLIB=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/job_1111


COMMAND_LINE="runLocal.sh $TASKLIB $INPUT_FILE_DIRECTORIES i$S3_ROOT $WORKING_DIR $TASKLIB/hello.R hello"

docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r28  $COMMAND_LINE

