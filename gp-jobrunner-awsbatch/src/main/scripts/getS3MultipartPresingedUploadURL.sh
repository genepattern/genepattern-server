#!/bin/bash

script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

python ${script_dir}/getS3MultipartPresingedUploadURL.py $@


