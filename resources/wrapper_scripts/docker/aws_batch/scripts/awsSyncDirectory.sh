#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

S3_ROOT=s3://moduleiotest

aws s3 sync  $S3_ROOT$1 $1 --profile genepattern
