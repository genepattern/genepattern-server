#!/bin/sh

TASKLIB=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r27/tests/survival_curve/job_1111


COMMAND_LINE="java -cp /build -DR_HOME=/packages/R-2.7.2/ -Dr_flags="'--no-save --quiet --slave --no-restore'" RunR $TASKLIB/SurvivalCurve.R SurvivalCurve surv.txt -c surv.cls surv time censor -fcls.clinical F automatic -lt -lc 1 1 -m 0 1 log 0 T left-bottom"

RHOME=/packages/R-2.7.2/

# note the crazy escaping of the quote string to get the rflags passed without being broken up by either docker or the script inside the container


# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# Make the input file directory since we need to put the script to execute in it
mkdir -p $WORKING_DIR
mkdir -p $WORKING_DIR/.gp_metadata
EXEC_SHELL=$WORKING_DIR/.gp_metadata/local_exec.sh

echo "#!/bin/bash\n" > $EXEC_SHELL
echo $COMMAND_LINE >>$EXEC_SHELL
echo "\n " >>$EXEC_SHELL

chmod a+x $EXEC_SHELL


REMOTE_COMMAND="runLocal.sh $TASKLIB $INPUT_FILE_DIRECTORIES $S3_ROOT $WORKING_DIR $EXEC_SHELL"

docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r27 $REMOTE_COMMAND

#echo $REMOTE_COMMAND
#docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR -it  liefeld/r27  bash
