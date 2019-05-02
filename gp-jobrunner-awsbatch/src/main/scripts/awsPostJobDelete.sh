#!/usr/bin/env bash
# used to clear out S3 cache when a job is deleted

shift

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

: ${S3_ROOT?not set}
: ${2?arg 2 not set}

#aws batch terminate-job --job-id $1 --reason "Cancelled from GenePattern"
aws s3 rm $S3_ROOT$2 --recursive

# also drop the meta dir. 
META_DIR=${2}.meta
aws s3 rm $S3_ROOT$META_DIR --recursive

