#!/bin/sh

TASKLIB=$PWD/src
INPUT_FILE_DIRECTORIES=$PWD/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$PWD/job_1111
RLIB=$PWD/rlib

RHOME=/packages/R-3.0.3/

# note the crazy escaping of the quote string to get the rflags passed without being broken up by either docker or the script inside the container
#COMMAND_LINE="java -cp /build -DR_HOME=$RHOME -Dr_flags=\"--no-save --quiet --slave --no-restore\" RunR $TASKLIB/hello.R hello"

COMMAND_LINE="Rscript  $TASKLIB/REVEALER_library.v5C.R  --ds1=$INPUT_FILE_DIRECTORIES/CTNBB1_transcriptional_reporter.gct  --target.name=BETA-CATENIN-REPORTER  --target.match=positive  --ds2=$INPUT_FILE_DIRECTORIES/CCLE_MUT_CNA_AMP_DEL_0.70_2fold.MC.gct  --seed.names=CTNNB1.MC_MUT  --max.n.iter=2  --n.markers=30  --locs.table.file=$INPUT_FILE_DIRECTORIES/hgnc_complete_set.Feb_20_2014.v2.txt  --count.thres.low=3  --count.thres.high=50  --pdf.output.file=BCAT_vs_MUT_CNA.pdf"



#docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR -v $RLIB:/build/rlib -w $WORKING_DIR -e LIBDIR=$TASKLIB -e R_LIBS=/build/rlib -e TASKLIB=$TASKLIB  liefeld/r30_cli runLocal.sh $COMMAND_LINE



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
docker run -v $RLIB:/usr/local/lib/R/site-library -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r30 $REMOTE_COMMAND

