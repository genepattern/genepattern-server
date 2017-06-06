#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#
TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/R30/tests/revealer
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111
RLIB=$TEST_ROOT/rlib


# /xchip/gpprod/servers/genepattern/gp/resources/wrapper_scripts/run-rscript.sh  -c  env-custom-centos5.sh  -l  /xchip/gpprod/servers/genepattern/taskLib/REVEALER.0.53.18220/  -p  /xchip/gpprod/servers/genepattern/gp/patches  -a  centos5  -v  3.0  --  /xchip/gpprod/servers/genepattern/taskLib/REVEALER.0.53.18220//REVEALER_library.v5C.R  --ds1=/xchip/gpprod/servers/genepattern/jobResults/1499734/CTNBB1_transcriptional_reporter.gct  --target.name=BETA-CATENIN-REPORTER  --target.match=positive  --ds2=/xchip/gpprod/servers/genepattern/jobResults/1499734/CCLE_MUT_CNA_AMP_DEL_0.70_2fold.MC.gct  --seed.names=CTNNB1.MC_MUT  --max.n.iter=2  --n.markers=30  --locs.table.file=/xchip/gpprod/servers/genepattern/jobResults/1499734/hgnc_complete_set.Feb_20_2014.v2.txt  --count.thres.low=3  --count.thres.high=50  --pdf.output.file=BCAT_vs_MUT_CNA.pdf
 
COMMAND_LINE="Rscript  $TASKLIB/REVEALER_library.v5C.R  --ds1=$INPUT_FILE_DIRECTORIES/CTNBB1_transcriptional_reporter.gct  --target.name=BETA-CATENIN-REPORTER  --target.match=positive  --ds2=$INPUT_FILE_DIRECTORIES/CCLE_MUT_CNA_AMP_DEL_0.70_2fold.MC.gct  --seed.names=CTNNB1.MC_MUT  --max.n.iter=2  --n.markers=30  --locs.table.file=$INPUT_FILE_DIRECTORIES/hgnc_complete_set.Feb_20_2014.v2.txt  --count.thres.low=3  --count.thres.high=50  --pdf.output.file=BCAT_vs_MUT_CNA.pdf"



docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR -v $RLIB:/build/rlib -w $WORKING_DIR -e LIBDIR=$TASKLIB -e R_LIBS=/build/rlib -e TASKLIB=$TASKLIB  liefeld/r30_cli runLocal.sh $COMMAND_LINE


