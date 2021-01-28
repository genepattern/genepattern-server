#!/bin/bash
echo "Args are $@"
source /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/activate base
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

python ${DIR}/presignUpload.py $@



