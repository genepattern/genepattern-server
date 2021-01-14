#!/bin/bash

script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"



echo "Using Python --> $@"
#source /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/activate base
python ${script_dir}/presignUpload.py $@

#echo "Using lambda" 
#source  ${DIR}/presignUpload.sh.sh $@




