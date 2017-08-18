#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

: ${S3_ROOT?not set}
: ${1?arg 1 not set}

aws s3 sync $S3_ROOT$1 $1
