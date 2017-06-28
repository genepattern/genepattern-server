#!/usr/bin/env bash

############################################################
# init-aws-cli-env.sh
#   Initialize the aws-cli environment 
#
# Note: this script is hard-coded to work on my Mac OS X dev laptop
# To configure for your server, edit the script ...
#   source  ~/.bash_profile
############################################################

# todo: gp-env integration, e.g.
#  aws-cmd=<run-with-env> -u miniconda2/4.3.13 -u aws-cli/1.11.87 aws

# -u miniconda2/4.3.13
MINICONDA2_HOME=/Broad/Applications/miniconda2
export PATH="${PATH}:${MINICONDA2_HOME}/bin"

# -u  aws-cli/1.11.87
source activate awscli

#export AWS_PROFILE_ARG=""
#export AWS_PROFILE_ARG="--region us-east-1"
export AWS_PROFILE_ARG="--profile genepattern"
