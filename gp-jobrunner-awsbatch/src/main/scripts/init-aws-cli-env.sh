#!/usr/bin/env bash

############################################################
# init-aws-cli-env.sh
#   Initialize the aws-cli environment 
#
# By default there will be no profile
#
# Note: this script is hard-coded to work on Peter Carr's Mac OS X dev laptop
# To configure for your server, edit this script.  If you already
# have the aws cli installed and its just always on your path you can
# uncomment the following line (and comment the miniconda lines out)
#
# source  ~/.bash_profile
#
############################################################

# todo: gp-env integration, e.g.
#  aws-cmd=<run-with-env> -u miniconda2/4.3.13 -u aws-cli/1.11.87 aws

# -u miniconda2/4.3.13
CONDA_PATH_BACKUP=/Applications/OpenCRAVAT.app/Contents/Resources/launchers:/Users/liefeld/.sdkman/candidates/grails/current/bin:/Users/liefeld/ana_conda_4/anaconda/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/Users/liefeld/tools/ant/apache-ant-1.10.1/bin
CONDA_PREFIX=/Users/liefeld/ana_conda_4/anaconda/envs/awscli
CONDA_DEFAULT_ENV=awscli
CONDA_PS1_BACKUP="\h:\W \u\$ "

# these 2 lines are for Peter Carr's laptop only
MINICONDA2_HOME=/Users/liefeld/ana_conda_4/anaconda/bin
export PATH="${PATH}:${MINICONDA2_HOME}/bin:/Users/liefeld/ana_conda_4/anaconda/envs/awscli/bin/"
# export PATH=$PATH:/Users/liefeld/ana_conda_4/anaconda/envs/awscli/bin:/Applications/OpenCRAVAT.app/Contents/Resources/launchers:/Users/liefeld/.sdkman/candidates/grails/current/bin:/Users/liefeld/ana_conda_4/anaconda/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/Users/liefeld/tools/ant/apache-ant-1.10.1/bin


# -u  aws-cli/1.11.87
source activate awscli

# set AWS_PROFILE if not already set
: ${AWS_PROFILE=genepattern}
# set S3_ROOT if not already set
: ${S3_ROOT=s3://moduleiotest}
# set JOB_QUEUE if not already set
: ${JOB_QUEUE=TedTest}
