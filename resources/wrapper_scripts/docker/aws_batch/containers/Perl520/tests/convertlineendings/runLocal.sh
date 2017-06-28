#!/bin/sh

#TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/Perl520/tests/convertlineendings
TEST_ROOT=$PWD
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1

COMMAND_LINE="perl $TASKLIB/to_host.pl $INPUT_FILE_DIRECTORIES/man2.cvt.txt aConvertedFile.txt"

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
docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/perl520_cli $REMOTE_COMMAND

