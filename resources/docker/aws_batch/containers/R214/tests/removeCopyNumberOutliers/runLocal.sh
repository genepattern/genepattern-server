#!/bin/sh

TASKLIB=$PWD/src
INPUT_FILE_DIRECTORIES=$PWD/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$PWD/job_1111

COMMAND_LINE="Rscript --no-save --quiet --slave --no-restore $TASKLIB/removeOutliers.R $TASKLIB/ $TASKLIB/userDIR  /patches  mynah.sorted --input $INPUT_FILE_DIRECTORIES/mynah.sorted.cn --trailingN 5 --replacement NA --mult_tol 4 --add_tol 0.3 --outputdir ./"

# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# Make the input file directory since we need to put the script to execute in it
echo "============== making dir $INPUT_FILE_DIRECTORIES"
mkdir -p $WORKING_DIR
mkdir -p $WORKING_DIR/.gp_metadata
EXEC_SHELL=$WORKING_DIR/.gp_metadata/local_exec.sh

echo "#!/bin/bash\n" > $EXEC_SHELL
echo $COMMAND_LINE >>$EXEC_SHELL
echo "\n " >>$EXEC_SHELL

chmod a+x $EXEC_SHELL

echo "============= wrote $EXEC_SHELL"

REMOTE_COMMAND="runLocal.sh $TASKLIB $INPUT_FILE_DIRECTORIES $S3_ROOT $WORKING_DIR $EXEC_SHELL" 

echo "Container will execute $REMOTE_COMMAND  <EOM>\n"
docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r214 $REMOTE_COMMAND

#docker run -it -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r214 bash


