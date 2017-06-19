#!/bin/sh

# Rscript src/SurvivalCurve.R SurvivalCurve data/surv.txt -c data/surv.cls surv time censor -fcls.clinical F automatic -lt -lc 1 1 -m 0 1 log 0 T left-bottom

# /Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve


TASKLIB=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve/job_1111



java -cp /build -DR_HOME=/packages/R-2.7.2/ -Dr_flags="'--no-save --quiet --slave --no-restore'" RunR $5


COMMAND_LINE="runLocal.sh $TASKLIB $INPUT_FILES_DIR $S3_ROOT $WORKING_DIR java -cp /build -DR_HOME=/packages/R-2.7.2/ -Dr_flags="'--no-save --quiet --slave --no-restore'" RunR $TASKLIB/SurvivalCurve.R SurvivalCurve surv.txt -c surv.cls surv time censor -fcls.clinical F automatic -lt -lc 1 1 -m 0 1 log 0 T left-bottom"

#docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld_r27  $COMMAND_LINE
echo $COMMAND_LINE

docker run -it -e CMD=$COMAND_LINE -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r27 /bin/bash

