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
aws s3 sync $S3_ROOT$TASKLIB $TASKLIB
ls $TASKLIB

 
# copy the inputs
mkdir -p $INPUT_FILES_DIR
aws s3 sync $S3_ROOT$INPUT_FILES_DIR $INPUT_FILES_DIR
ls $INPUT_FILES_DIR

# switch to the working directory
mkdir -p $WORKING_DIR
cd $WORKING_DIR 
# used for stderr, stdout and exitcode files
mkdir .gp_metadata


# run the module
echo $5
$5 >$STDOUT_FILENAME 2>$STDERR_FILENAME
echo "{ \"exit_code\": $? }">$EXITCODE_FILENAME

# send the generated files back
aws s3 sync . $S3_ROOT$WORKING_DIR 
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB
