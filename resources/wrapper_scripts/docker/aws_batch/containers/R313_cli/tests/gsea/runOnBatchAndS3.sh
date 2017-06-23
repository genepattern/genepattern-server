#!/bin/sh

TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/R313_cli/tests/gsea
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111
RLIB=$TEST_ROOT/../rlib

COMMAND_LINE="java -Djava.util.prefs.PreferencesFactory=org.genepattern.modules.gsea.DisabledPreferencesFactory -Ddebug=true -Dgp.chip=ftp://ftp.broadinstitute.org/pub/gsea/annotations/Hu6800.chip -Dgp.zip=tedsGSEA -Dmkdir=false -Djava.awt.headless=true -cp $TASKLIB/commons-net-ftp-2.0.jar:$TASKLIB/commons-net-2.0.jar:$TASKLIB/commons-cli-1.2.jar:$TASKLIB/gp-gsea.jar:$TASKLIB/gsea2-2.0.13.jar:$TASKLIB/ant.jar org.genepattern.modules.gsea.GseaWrapper -res $INPUT_FILE_DIRECTORIES/all_aml_test.gct -cls $INPUT_FILE_DIRECTORIES/all_aml_test.cls -collapse true -mode Max_probe -norm meandiv -nperm 1000 -permute phenotype -rnd_type no_balance -scoring_scheme weighted -rpt_label my_analysis -metric Signal2Noise -sort real -order descending -include_only_symbols true -make_sets true -median false -num 100 -plot_top_x 20 -rnd_seed timestamp -save_rnd_lists false -set_max 500 -set_min 15 -zip_report false -out . -gui false -chip Hu6800 -gmx c2.all.v5.2.symbols.gmt"


JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_GSEA_R313_$1
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


