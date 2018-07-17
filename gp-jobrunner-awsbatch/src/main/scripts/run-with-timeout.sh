#!/usr/bin/env bash

############################################################
# run-with-timeout.sh
#   Example wrapper script with configurable timeout
#
# Usage:
#   sh run-with-timeout.sh \
#     timeout-sec stdout-file stderr-file \
#     cmd [args]*
#
# Includes:
#   gp-timeout.sh
############################################################

__dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${__dir}/gp-timeout.sh"
run_with_timeout "${@}"
