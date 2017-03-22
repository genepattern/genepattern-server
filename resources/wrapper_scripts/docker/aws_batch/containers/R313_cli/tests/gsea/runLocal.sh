#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch
TASKLIB=$TEST_ROOT/R313_cli/tests/gsea/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/R313_cli/tests/gsea/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/R313_cli/tests/gsea/job_1111



COMMAND_LINE="java -Djava.util.prefs.PreferencesFactory=org.genepattern.modules.gsea.DisabledPreferencesFactory -Ddebug=true -Dgp.chip=ftp://ftp.broadinstitute.org/pub/gsea/annotations/Hu6800.chip -Dgp.zip=tedsGSEA -Dmkdir=false -Djava.awt.headless=true -cp $TASKLIB/commons-net-ftp-2.0.jar:$TASKLIB/commons-net-2.0.jar:$TASKLIB/commons-cli-1.2.jar:$TASKLIB/gp-gsea.jar:$TASKLIB/gsea2-2.0.13.jar:$TASKLIB/ant.jar org.genepattern.modules.gsea.GseaWrapper -res $INPUT_FILE_DIRECTORIES/all_aml_test.gct -cls $INPUT_FILE_DIRECTORIES/all_aml_test.cls -collapse true -mode Max_probe -norm meandiv -nperm 1000 -permute phenotype -rnd_type no_balance -scoring_scheme weighted -rpt_label my_analysis -metric Signal2Noise -sort real -order descending -include_only_symbols true -make_sets true -median false -num 100 -plot_top_x 20 -rnd_seed timestamp -save_rnd_lists false -set_max 500 -set_min 15 -zip_report false -out . -gui false -chip Hu6800 -gmx c2.all.v5.2.symbols.gmt"


#echo "docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r313_cli"
docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/r313_cli $COMMAND_LINE




