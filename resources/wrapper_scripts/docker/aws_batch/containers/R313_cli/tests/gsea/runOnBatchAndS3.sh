#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TASKLIB=/Users/liefeld/dockerProjects/modules/R313_cli/tests/gsea/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/dockerProjects/modules/R313_cli/tests/gsea/data
JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_GSEA_R313_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/dockerProjects/modules/R313_cli/tests/gsea/job_1111

COMMAND_LINE="java -Djava.util.prefs.PreferencesFactory=org.genepattern.modules.gsea.DisabledPreferencesFactory -Ddebug=true -Dgp.chip=ftp://ftp.broadinstitute.org/pub/gsea/annotations/Hu6800.chip -Dgp.zip=tedsGSEA -Dmkdir=false -Djava.awt.headless=true -cp $TASKLIB/commons-net-ftp-2.0.jar:$TASKLIB/commons-net-2.0.jar:$TASKLIB/commons-cli-1.2.jar:$TASKLIB/gp-gsea.jar:$TASKLIB/gsea2-2.0.13.jar:$TASKLIB/ant.jar org.genepattern.modules.gsea.GseaWrapper -res $INPUT_FILE_DIRECTORIES/all_aml_test.gct -cls $INPUT_FILE_DIRECTORIES/all_aml_test.cls -collapse true -mode Max_probe -norm meandiv -nperm 1000 -permute phenotype -rnd_type no_balance -scoring_scheme weighted -rpt_label my_analysis -metric Signal2Noise -sort real -order descending -include_only_symbols true -make_sets true -median false -num 100 -plot_top_x 20 -rnd_seed timestamp -save_rnd_lists false -set_max 500 -set_min 15 -zip_report false -out . -gui false -chip Hu6800 -gmx c2.all.v5.2.symbols.gmt"

#
# Copy the input files to S3 using the same path
#
aws s3 sync $INPUT_FILE_DIRECTORIES $S3_ROOT$INPUT_FILE_DIRECTORIES --profile genepattern
aws s3 sync $TASKLIB $S3_ROOT$TASKLIB --profile genepattern

#       --container-overrides memory=2000 \


aws batch submit-job \
      --job-name $JOB_ID \
      --job-queue $JOB_QUEUE \
      --container-overrides 'memory=3600' \
      --job-definition $JOB_DEFINITION_NAME \
      --parameters taskLib=$TASKLIB,inputFileDirectory=$INPUT_FILE_DIRECTORIES,s3_root=$S3_ROOT,working_dir=$WORKING_DIR,exe1="$COMMAND_LINE"  \
      --profile genepattern

# may want to pipe the submit output through this to extract the job ID for checking status
# | python -c "import sys, json; print json.load(sys.stdin)['jobId']"




# wait for job completion TBD
#
#check status 
# aws batch describe-jobs --jobs 07f93b66-f6c0-47e5-8481-4a04722b7c91 --profile genepattern
#

#
# Copy the output of the job back to our local working dir from S3
#
#aws s3 sync $S3_ROOT$WORKING_DIR $WORKING_DIR --profile genepattern

