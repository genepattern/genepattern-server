#!/usr/bin/env bash
. /broad/tools/scripts/useuse
use R-2.7

# Needed for FLAME modules
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:/xchip/gpdev/shared_libraries"

#debug echo "running cmd: $@"
"$@"
