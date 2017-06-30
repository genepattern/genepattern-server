#!/bin/bash

# strip off spaces if present
TASKLIB="$(echo -e "${1}" | tr -d '[:space:]')"
INPUT_FILES_DIR="$(echo -e "${2}" | tr -d '[:space:]')"
S3_ROOT="$(echo -e "${3}" | tr -d '[:space:]')"
WORKING_DIR="$(echo -e "${4}" | tr -d '[:space:]')"
EXECUTABLE=$5

#
# assign filenames for STDOUT and STDERR if not already set
#
: ${STDOUT_FILENAME=.gp_metadata/stdout.txt}
: ${STDERR_FILENAME=.gp_metadata/stderr.txt}
: ${EXITCODE_FILENAME=.gp_metadata/exit_code.txt}

# echo out params
echo working dir is  -$WORKING_DIR- 
echo Task dir is -$TASKLIB-
echo executable is -$5-
echo S3_ROOT is -$S3_ROOT-
echo input files location  is -$INPUT_FILES_DIR-

# copy the source over from tasklib
mkdir -p $TASKLIB
echo "AWS SYNC $S3_ROOT$TASKLIB $TASKLIB"
aws s3 sync $S3_ROOT$TASKLIB $TASKLIB --quiet
ls $TASKLIB

 
# copy the inputs
mkdir -p $INPUT_FILES_DIR
echo "PERFORMING aws s3 sync $S3_ROOT$INPUT_FILES_DIR $INPUT_FILES_DIR"
aws s3 sync $S3_ROOT$INPUT_FILES_DIR $INPUT_FILES_DIR --quiet
ls $INPUT_FILES_DIR

# switch to the working directory and sync it up
echo "PERFORMING aws s3 sync $S3_ROOT$WORKING_DIR $WORKING_DIR "
aws s3 sync $S3_ROOT$WORKING_DIR $WORKING_DIR --quiet
aws s3 sync $S3_ROOT$WORKING_DIR/.gp_metadata $WORKING_DIR/.gp_metadata --quiet
chmod a+rwx $WORKING_DIR/.gp_metadata/*
echo "POST SYNC WORKING_DIR/.gp_metadata contents are"

cd $WORKING_DIR

# run the module
echo "PERFORMING $5"
$5 >$STDOUT_FILENAME 2>$STDERR_FILENAME
echo "{ \"exit_code\": $? }">$EXITCODE_FILENAME

# send the generated files back
echo "PERFORMING aws s3 sync $WORKING_DIR $S3_ROOT$WORKING_DIR"
aws s3 sync $WORKING_DIR $S3_ROOT$WORKING_DIR --quiet
echo "PERFORMING aws s3 sync $TASKLIB $S3_ROOT$TASKLIB"
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --quiet

