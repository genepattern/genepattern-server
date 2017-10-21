#!/bin/sh

TEST_ROOT=$PWD
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_R313_GSEA_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111

COMMAND_LINE="java -Djava.util.prefs.PreferencesFactory=org.genepattern.modules.gsea.DisabledPreferencesFactory -Ddebug=true -Dgp.chip=ftp://ftp.broadinstitute.org/pub/gsea/annotations/Hu6800.chip -Dgp.zip=tedsGSEA -Dmkdir=false -Djava.awt.headless=true -cp $TASKLIB/commons-net-ftp-2.0.jar:$TASKLIB/commons-net-2.0.jar:$TASKLIB/commons-cli-1.2.jar:$TASKLIB/gp-gsea.jar:$TASKLIB/gsea2-2.0.13.jar:$TASKLIB/ant.jar org.genepattern.modules.gsea.GseaWrapper -res $INPUT_FILE_DIRECTORIES/all_aml_test.gct -cls $INPUT_FILE_DIRECTORIES/all_aml_test.cls -collapse true -mode Max_probe -norm meandiv -nperm 1000 -permute phenotype -rnd_type no_balance -scoring_scheme weighted -rpt_label my_analysis -metric Signal2Noise -sort real -order descending -include_only_symbols true -make_sets true -median false -num 100 -plot_top_x 20 -rnd_seed timestamp -save_rnd_lists false -set_max 500 -set_min 15 -zip_report false -out . -gui false -chip Hu6800 -gmx c2.all.v5.2.symbols.gmt"


RLIB=$TEST_ROOT/../rlib


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
docker run -m 2g -v $RLIB:/usr/local/lib/R/site-library -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r313_cli $REMOTE_COMMAND

