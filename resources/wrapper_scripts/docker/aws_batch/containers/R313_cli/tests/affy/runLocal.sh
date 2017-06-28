#!/bin/sh

TEST_ROOT=$PWD
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_R313_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_12345

COMMAND_LINE="Rscript --no-save --quiet --slave --no-restore $TASKLIB/run_gp_affyst_efc.R $TASKLIB --input.file=$INPUT_FILE_DIRECTORIES/inputFileList.txt --normalize=yes --background.correct=no --qc.plot.format=skip --annotate.rows=yes --output.file.base=tedsOutputFile"

#docker run -m 5g -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r313_cli $COMMAND_LINE

RLIB=$TEST_ROOT/containers/R313_cli/tests/rlib


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
docker run -m 5g -v $RLIB:/usr/local/lib/R/site-library -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r313_cli $REMOTE_COMMAND

