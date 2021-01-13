#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "Using Python"
source /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/activate base
python ${DIR}/presignUpload.py $@

#echo "Using lambda" 
#source  ${DIR}/presignUpload.sh.sh $@




