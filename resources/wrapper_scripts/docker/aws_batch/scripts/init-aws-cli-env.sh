#!/usr/bin/env bash

############################################################
# init-aws-cli-env.sh
#   Initialize the aws-cli environment 
############################################################

# todo: gp-env integration, e.g.
#  aws-cmd=<run-with-env> -u miniconda2/4.3.13 -u aws-cli/1.11.87 aws

# hard-coded for my Mac OS X dev machine
# -u miniconda2/4.3.13

MINICONDA2_HOME=/Broad/Applications/miniconda2
export PATH="${PATH}:${MINICONDA2_HOME}/bin"

# -u  aws-cli/1.11.87
source activate awscli
