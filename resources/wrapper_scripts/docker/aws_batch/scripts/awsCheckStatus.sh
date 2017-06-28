#!/usr/bin/env bash

# initialize aws-cli environment
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${script_dir}/init-aws-cli-env.sh"

#aws batch describe-jobs --jobs $1 $AWS_PROFILE_ARG | python -c "import sys, json; print( json.load(sys.stdin)['jobs'][0]['status'])"
aws batch describe-jobs --jobs $1 $AWS_PROFILE_ARG
