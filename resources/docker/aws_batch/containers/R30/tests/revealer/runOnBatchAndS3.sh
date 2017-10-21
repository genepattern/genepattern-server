#!/bin/sh

TEST_ROOT=$PWD
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111
RLIB=$TEST_ROOT/rlib


COMMAND_LINE="Rscript  $TASKLIB/REVEALER_library.v5C.R  --ds1=$INPUT_FILE_DIRECTORIES/CTNBB1_transcriptional_reporter.gct  --target.name=BETA-CATENIN-REPORTER  --target.match=positive  --ds2=$INPUT_FILE_DIRECTORIES/CCLE_MUT_CNA_AMP_DEL_0.70_2fold.MC.gct  --seed.names=CTNNB1.MC_MUT  --max.n.iter=2  --n.markers=30  --locs.table.file=$INPUT_FILE_DIRECTORIES/hgnc_complete_set.Feb_20_2014.v2.txt  --count.thres.low=3  --count.thres.high=50  --pdf.output.file=BCAT_vs_MUT_CNA.pdf"

JOB_DEFINITION_NAME="R30_Generic"
JOB_ID=gp_job_REVEALER_R30_$1
JOB_QUEUE=TedTest


# ##### NEW PART FOR SCRIPT INSTEAD OF COMMAND LINE ################################
# Make the input file directory since we need to put the script to execute in it
mkdir -p $WORKING_DIR/.gp_metadata

EXEC_SHELL=$WORKING_DIR/.gp_metadata/exec.sh

echo "#!/bin/bash\n" > $EXEC_SHELL
#echo "echo \"$COMMAND_LINE\"" >>$EXEC_SHELL
echo $COMMAND_LINE >>$EXEC_SHELL 
echo "\n " >>$EXEC_SHELL 

chmod a+rwx $EXEC_SHELL
chmod -R a+rwx $WORKING_DIR

REMOTE_COMMAND="runS3OnBatch.sh $TASKLIB $INPUT_FILE_DIRECTORIES $S3_ROOT $WORKING_DIR $EXEC_SHELL"
# note the batch submit now uses REMOTE_COMMAND instead of COMMAND_LINE 

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern
aws s3 sync $WORKING_DIR $S3_ROOT$WORKING_DIR --profile genepattern 

######### end new part for script #################################################


#       --container-overrides memory=2000 \

aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --container-overrides 'memory=3600' \
      --job-definition $JOB_DEFINITION_NAME \
      --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORIES,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$REMOTE_COMMAND"  \
      --profile genepattern


