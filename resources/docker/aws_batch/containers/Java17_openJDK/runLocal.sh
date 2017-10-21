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

cd $WORKING_DIR

# run the module
shift
shift
shift
shift

echo "========== DEBUG inside container ================="
echo $@
echo "====== END DEBUG ================="

"$@"  >$STDOUT_FILENAME 2>$STDERR_FILENAME



