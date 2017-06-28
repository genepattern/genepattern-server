#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

aws batch terminate-job --job-id $1 --reason "Cancelled from GenePattern" $AWS_PROFILE_ARG
