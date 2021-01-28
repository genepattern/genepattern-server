#!/bin/bash
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"


aws s3api complete-multipart-upload --cli-input-json ${1}  


# remove the temp input file to avoid wasting space
rm $IN_FILE
