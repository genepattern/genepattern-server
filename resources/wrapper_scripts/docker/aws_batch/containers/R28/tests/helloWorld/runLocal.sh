#!/bin/sh

# Rscript src/SurvivalCurve.R SurvivalCurve data/surv.txt -c data/surv.cls surv time censor -fcls.clinical F automatic -lt -lc 1 1 -m 0 1 log 0 T left-bottom

# /Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/survival_curve


TASKLIB=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/r28/tests/helloWorld/job_1111

RHOME=/packages/R-2.8.1/
#RFLAGS='--no-save --quiet --slave --no-restore'


# note the crazy escaping of the quote string to get the rflags passed without being broken up by either docker or the script inside the container

COMMAND_LINE="runLocal.sh $TASKLIB $INPUT_FILE_DIRECTORIES $S3_ROOT $WORKING_DIR java -cp /build -DR_HOME=$RHOME -Dr_flags=\"--no-save --quiet --slave --no-restore\" RunR $TASKLIB/hello.R hello"

# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# Make the input file directory since we need to put the script to execute in it
mkdir -p $INPUT_FILE_DIRECTORIES
mkdir -p $INPUT_FILE_DIRECTORIES/.gp_metadata

aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES

echo "#!/bin/bash\n" > $INPUT_FILE_DIRECTORIES/.gp_metadata/exec.sh
echo "echo \"$COMMAND_LINE\"" >>$INPUT_FILE_DIRECTORIES/.gp_metadata/exec.sh
echo $COMMAND_LINE >>$INPUT_FILE_DIRECTORIES/.gp_metadata/exec.sh
echo "\n " >>$INPUT_FILE_DIRECTORIES/gp_metadata/exec.sh

chmod a+x $INPUT_FILE_DIRECTORIES/.gp_metadata/exec.sh

echo "host WROTE  $INPUT_FILE_DIRECTORIES/.gp_metadata/exec.sh"

REMOTE_COMMAND="$INPUT_FILE_DIRECTORIES/.gp_metadata/exec.sh"
# note the batch submit now uses REMOTE_COMMAND instead of COMMAND_LINE


#declare -a arr=("/usr/local/bin/runLocal.sh" 
#                "${TASKLIB}" 
#	        "${INPUT_FILE_DIRECTORIES}" 
#		"${S3_ROOT}" 
#		"${WORKING_DIR}" 
#		"java" 
#		"-cp" 
#		"/build" 
#		"-DR_HOME=${RHOME}" 
#		"-Dr_flags=--no-save --quiet --slave --no-restore" 
#		"RunR" 
#		"${TASKLIB}/hello.R" 
#		"hello")
#docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r28 "${arr[@]}"

echo "Container will execute $REMOTE_COMMAND  <EOM>\n"
docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r28 "$REMOTE_COMMAND"

