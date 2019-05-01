# used to clear out S3 cache when a job is deleted

#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

: ${S3_ROOT?not set}
: ${2?arg 2 not set}

#aws batch terminate-job --job-id $1 --reason "Cancelled from GenePattern"
aws s3 rm $S3_ROOT$2 --recursive

