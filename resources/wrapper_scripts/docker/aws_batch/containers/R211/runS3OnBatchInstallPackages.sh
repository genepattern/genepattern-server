#!/bin/bash

# strip off spaces if present
TASKLIB="$(echo -e "${1}" | tr -d '[:space:]')"
INPUT_FILES_DIR="$(echo -e "${2}" | tr -d '[:space:]')"
S3_ROOT="$(echo -e "${3}" | tr -d '[:space:]')"
WORKING_DIR="$(echo -e "${4}" | tr -d '[:space:]')"
EXECUTABLE=$5

: ${R_LIBS_S3=/genepattern-server/Rlibraries/R27/rlibs}

export R_LIBS=/usr/local/lib/R/site-library

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
echo R_LIBS is --$R_LIBS

# copy the source over from tasklib
mkdir -p $TASKLIB
aws s3 sync $S3_ROOT$TASKLIB $TASKLIB --quiet
ls $TASKLIB

 
# copy the inputs
mkdir -p $INPUT_FILES_DIR
aws s3 sync $S3_ROOT$INPUT_FILES_DIR $INPUT_FILES_DIR --quiet
ls $INPUT_FILES_DIR

# switch to the working directory
mkdir -p $WORKING_DIR
cd $WORKING_DIR 
# used for stderr, stdout and exitcode files
mkdir .gp_metadata

##################################################
# MODIFICATION FOR R PACKAGE INSTALLATION
##################################################
# mount pre-compiled libs from S3
aws s3 sync $S3_ROOT$R_LIBS_S3 $R_LIBS --quiet

if [ -f "$TASKLIB/r.package.info" ]
then
	echo "$TASKLIB/r.package.info found."
	Rscript /build/source/installPackages.R $TASKLIB/r.package.info
else
	echo "$TASKLIB/r.package.info not found."
fi


# run the module
echo $5
$5 >$STDOUT_FILENAME 2>$STDERR_FILENAME
echo "{ \"exit_code\": $? }">$EXITCODE_FILENAME

# send the generated files back
aws s3 sync . $S3_ROOT$WORKING_DIR 
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB
aws s3 sync $R_LIBS $S3_ROOT$R_LIBS_S3
