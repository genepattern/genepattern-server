#!/bin/sh

#
# parameters to this come from the JobRunner implementation
# for the AWS install
#
# current arg is just job #
#

TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/containers/gistic/tests/gistic_2_23
TASKLIB=$TEST_ROOT/src
INPUT_FILE_DIRECTORIES=$TEST_ROOT/data
S3_ROOT=s3://moduleiotest
WORKING_DIR=$TEST_ROOT/job_1111


# <libdir>gp_gistic2_from_seg -b . -seg <seg.file> -mk <markers.file> -cnv <cnv.file> -refgene <refgene.file> -ta <amplifications.threshold> -td <deletions.threshold> -js <join.segment.size> -qvt <q-value.threshold> -rx <remove.X> -cap <cap.val> -conf <confidence.level> -genegistic <gene.gistic> -broad <run.broad.analysis> -brlen <focal.length.cutoff> -maxseg <max.sample.segs> -armpeel <arm.peel> -scent <sample.center> -gcm <gene.collapse.method> -arb 1 -peak_type robust -fname <output.prefix> -genepattern 1 -twosides 0 -saveseg 0 -savedata 0 -smalldisk 0 -smallmem 1 -savegene 1 -v 0


COMMAND_LINE="$TASKLIB/gp_gistic2_from_seg -b . -seg $INPUT_FILE_DIRECTORIES/segmentationfile.txt -mk $INPUT_FILE_DIRECTORIES/markersfile.txt -cnv $INPUT_FILE_DIRECTORIES/cnvfile.txt -refgene $INPUT_FILE_DIRECTORIES/Human_Hg19.mat -td 0.1 -js 4 -qvt 0.25 -rx 1 -cap 1.5 -conf 0.90 -genegistic 1 -broad 0 -brien 0.50 -maxseg 2500 -ampeel 0 -scent median -gcm extreme -arb 1 -peak_type robust -fname segmentationfile -genepattern 1 -twosides 0 -saveseg 0 -savedata 0 -smalldisk 0 -smallmem 1 -savegene 1 -v 0 "


docker run -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR -w $WORKING_DIR liefeld/gistic /home/gistic/run.sh $COMMAND_LINE


#docker run -it -v $TASKLIB:$TASKLIB -v $INPUT_FILE_DIRECTORIES:$INPUT_FILE_DIRECTORIES -v $WORKING_DIR:$WORKING_DIR  liefeld/gistic /home/gistic/run.sh /bin/bash


