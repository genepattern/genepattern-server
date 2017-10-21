#!/bin/sh

TASKLIB=$PWD/src
INPUT_FILE_DIRECTORIES=$PWD/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$PWD/job_1112

COMMAND_LINE="java -Dlibdir\=$TASKLIB -cp $TASKLIB/gp-modules.jar:$TASKLIB/commons-math-1.2.jar:$TASKLIB/trove.jar:$TASKLIB/Jama-1.0.2.jar:$TASKLIB/colt.jar:$TASKLIB/jsci-core.jar org.broadinstitute.marker.MarkerSelection $INPUT_FILE_DIRECTORIES/all_aml_train.gct $INPUT_FILE_DIRECTORIES/all_aml_train.cls 100 2 cms_outfile false false no 1 12345  false false -lfalse  "

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
docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r28 $REMOTE_COMMAND

