#!/bin/sh

TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/R215/tests/rankNormalize
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111
RLIB=$TEST_ROOT/rlib


COMMAND_LINE="Rscript $TASKLIB/run_rank_normalize.R $TASKLIB --input.file=$INPUT_FILE_DIRECTORIES/all_aml_train.gct --output.file.name=all_aml_train.NORM.gct"

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

echo "Container will execute $REMOTE_COMMAND  <EOM>\n"
docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r215 $REMOTE_COMMAND

