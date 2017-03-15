#!/bin/bash

TASKLIB=/Users/liefeld/dockerProjects/modules/gsea/src
INPUT_FILE_DIRECTORIES=/Users/liefeld/dockerProjects/modules/gsea/data
JOB_DEFINITION_NAME="R313_Generic"
JOB_ID=gp_job_R313_$1
JOB_QUEUE=TedTest
S3_ROOT=s3://moduleiotest
WORKING_DIR=/Users/liefeld/dockerProjects/modules/gsea/job_1111


COMMAND_LINE="java -Djava.util.prefs.PreferencesFactory=org.genepattern.modules.gsea.DisabledPreferencesFactory -Ddebug=true -Dgp.chip=ftp://ftp.broadinstitute.org/pub/gsea/annotations/Hu6800.chip -Dgp.zip=tedsGSEA -Dmkdir=false -Djava.awt.headless=true -cp $TASKLIB/commons-net-ftp-2.0.jar:$TASKLIB/commons-net-2.0.jar:$TASKLIB/commons-cli-1.2.jar:$TASKLIB/gp-gsea.jar:$TASKLIB/gsea2-2.0.13.jar:$TASKLIB/ant.jar org.genepattern.modules.gsea.GseaWrapper -res $INPUT_FILE_DIRECTORIES/all_aml_test.gct -cls $INPUT_FILE_DIRECTORIES/all_aml_test.cls -collapse true -mode Max_probe -norm meandiv -nperm 1000 -permute phenotype -rnd_type no_balance -scoring_scheme weighted -rpt_label my_analysis -metric Signal2Noise -sort real -order descending -include_only_symbols true -make_sets true -median false -num 100 -plot_top_x 20 -rnd_seed timestamp -save_rnd_lists false -set_max 500 -set_min 15 -zip_report false -out . -gui false -chip Hu6800 -gmx c2.all.v5.2.symbols.gmt"


echo $COMMAND_LINE 

docker run -it -v /Users/liefeld/dockerProjects/modules/gsea/src:/Users/liefeld/dockerProjects/modules/gsea/src -v /Users/liefeld/dockerProjects/modules/gsea/data:/Users/liefeld/dockerProjects/modules/gsea/data -v /Users/liefeld/dockerProjects/modules/gsea/job_1111:/Users/liefeld/dockerProjects/modules/gsea/job_1111  liefeld/r313_cli /bin/bash
#

