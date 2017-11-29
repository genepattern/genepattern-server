#!/usr/bin/env bash

#
# shunit2 test cases for aws batch scripts
#

# called once before running any test
oneTimeSetUp()  {
  # path to this directory
  readonly __test_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  # path to aws-batch-scripts ../../main/scripts
  readonly __aws_batch_scripts_dir="$(cd "${__test_script_dir}/../../main/scripts" && pwd)"
  # path to wrapper-scripts ../../../../resources/wrapper_scripts
  readonly __gp_wrapper_scripts_dir="$(cd "${__test_script_dir}/../../../../resources/wrapper_scripts" && pwd)"
  readonly __gp_common_script="${__gp_wrapper_scripts_dir}/gp-common.sh";

  # source 'gp-common.sh'
  if [ -f "${__gp_common_script}" ]; then
    source "${__gp_common_script}";
  else
    fail "gp-common-script not found: ${__gp_common_script}";
  fi
  strict_mode
  # disable exit on error for unit testing
  set +o errexit
  
  : ${WORKING_DIR=.}
  : ${GP_METADATA_DIR=.}
  : ${GP_METADATA_DIR=$WORKING_DIR/.gp_metadata}
  CMD_LOG=${GP_METADATA_DIR}/aws_cmd.log
}

setUp() {
  unset GP_JOB_MEMORY_MB;
  unset GP_JOB_CPU_COUNT;
}


#
# duplicate declaration of functions in runOnBatch.sh
#

function cmd_log() {
  echo "$1" >> ${CMD_LOG-/dev/stdout} 2>&1
}

# Is $1 an integer
function is_int() {
  if [ $# -eq 0 ]; then return 1; fi 
  local arg;
  printf -v arg '%d\n' "${1:-x}" 2>/dev/null;
}

# Is $1 an integer, optionally in the given range
# Usage:
#   in_range arg [min] [max] 
# Return true if arg is an integer >= min and <= max
#
function in_range() { 
  # arg1 must be an integer
  [ $# -ge 1 ] && is_int "${1}" || return 1;
  local arg=$1;

  # optional min range, arg2
  #   note: '-z string', true if the length of string is zero.
  if ! [ -z ${2:+x} ]; then
    # warn: invalid arg, $2 is not an integer
    if ! is_int $2; then return 1; fi
    if (( $arg < $2 )); then return 1; fi
  fi

  # optional max range, arg3
  if ! [ -z ${3:+x} ]; then
    # warn: invalid arg, $3 is not an integer
    if ! is_int $3; then return 1; fi
    if (( $arg > $3 )); then return 1; fi
  fi
}

# Helper method to validate job.memory before 
#   submitting an aws batch job 
# Usage:
#   is_valid_mem_flag mem [min, default=400] [max, default=2000000000]
# Example:
#   # units are in MiB
#   : ${GP_JOB_MEMORY_MB=:4000}
#   mem_flag="";
#   if is_valid_mem_flag $GP_JOB_MEMORY_MB; then
#     mem_flag="memory=${GP_JOB_MEMORY_MB}";
#   fi


# Helper method to set memory args for an aws batch job.
#   Units are in MiB.
# Usage:
#   mem_flag [custom-job-memory-mb, default=$GP_JOB_MEMORY_MB]
# Example: with no arg, use GP_JOB_MEMORY_MB environment variable
#   : ${GP_JOB_MEMORY_MB=:4000}
#   local arg="$(mem_flag)"
# Example: set the memory as an arg (MiB)
#   local arg=$(mem_flag 4000)
function mem_flag() {
  local mem="${1:-${GP_JOB_MEMORY_MB-(not set)}}"
  local name="GP_MEMORY_MB";
  if [ "$#" -eq 1 ]; then
    name="\$1";
  fi
  cmd_log "calculating --container-overrides for memory ...";
  cmd_log "    $name='${mem}'";
  local min_mem="400";
  local max_mem="1000000";
  if in_range "${mem:-x}" "${min_mem}" "${max_mem}"; then
    echo "memory=${mem},";
  fi
  echo "";
}

# simpler implementation, ignores args and uses the GP_JOB_MEMORY environment variable
function mem_flag_env() {
  cmd_log "calculating --container-overrides for memory ...";
  cmd_log "    GP_JOB_MEMORY_MB='${GP_JOB_MEMORY_MB-(not set)}'";
  local min_mem="400";
  local max_mem="1000000";
  if in_range "${GP_JOB_MEMORY_MB:-x}" "${min_mem}" "${max_mem}"; then
    echo "memory=${GP_JOB_MEMORY_MB},";
  fi
  echo "";
}

# example of indirect reference, pass in the name of the environment variable as an arg
# Usage:
#   mem_flag_by_ref [varname]
function mem_flag_by_ref() {
  local name=${1:-GP_JOB_MEMORY_MB};
  cmd_log "calculating --container-overrides for memory ...";
  cmd_log "    $name='${!name-(not set)}'";
  local min_mem="400";
  local max_mem="1000000";
  if in_range "${!name:-x}" "${min_mem}" "${max_mem}"; then
    echo "memory=${!name},";
  fi
  echo "";
}

function vcpus_flag() {
  cmd_log "calculating --container-overrides for vcpus ...";
  cmd_log "    GP_JOB_CPU_COUNT='${GP_JOB_CPU_COUNT-(not set)}'";
  local min_vcpu="1";
  local max_vcpu="256";
  if in_range "${GP_JOB_CPU_COUNT:-x}" "${min_vcpu}" "${max_vcpu}"; then
    echo "vcpus=${GP_JOB_CPU_COUNT},";
  fi
  echo "";
}

#
# custom assertions
#
function assertIsFunctionDefined() {
  [ "$(type -t $1)" = "function" ] \
    || fail "'$1', function not defined";
}

function assertIsInt() {
  assertTrue "  is_int '$1', expecting true" \
    "is_int $1";
}

function assertNotIsInt() {
  assertFalse "  is_int '$1', expecting false" \
    "is_int $1";
}

function assertInRange() {
  if ! in_range "${@}"; then
    fail "  in_range '$*', expecting true";
  fi
}

function assertNotInRange() {
  if in_range "${@}"; then
    fail "  in_range '$*', expecting false";
  fi
}

#
# define the tests
#
test_path_to_scripts() {
  echo "    wrapper-scripts: ${__gp_wrapper_scripts_dir}"
  echo "  aws-batch-scripts: ${__aws_batch_scripts_dir}"
}

test_is_int() {
  assertIsFunctionDefined "is_int";

  assertIsInt 0;
  assertIsInt -1;
  assertIsInt 1;
  assertIsInt "0";
  assertIsInt "-1";
  assertIsInt "1";
  assertIsInt "999999";
  assertIsInt "-999999";
      
  assertFalse "no arg, expecting false" is_int; 
  
  assertNotIsInt 0.1;
  assertNotIsInt -1.5;
  assertNotIsInt 3.14159;
  assertNotIsInt "3.14";
  assertNotIsInt "a word";
  assertNotIsInt "";
}

test_in_range() {
  assertIsFunctionDefined "in_range";

  # assertFalse "in_range '0', expecting false" "in_range 0";
  assertInRange 1;
  assertInRange 0;
  assertInRange -1;
  
  local min_mem="400";
  local max_mem="1000000";

  assertInRange $min_mem $min_mem $max_mem;
  assertInRange $max_mem $min_mem $max_mem;

  assertNotInRange 0 $min_mem $max_mem;
  assertNotInRange 399 $min_mem $max_mem;
  assertNotInRange 2000000 $min_mem $max_mem;

  # special-case, value is not an int
  assertNotInRange 1000.25 $min_mem $max_mem;
  # special-case, min-range is not an int
  assertNotInRange 400 100.15 $max_mem;
  # special-case, max-range is not an int
  assertNotInRange 400 $min_mem 2000.15;
}

test_mem_flag() {
  assertIsFunctionDefined "cmd_log";
  assertIsFunctionDefined "mem_flag";

  assertEquals "by default, no mem flag" \
    "" "$(mem_flag)"

  export GP_JOB_MEMORY_MB="";
  assertEquals "ignore empty memory" \
    "" "$(mem_flag)"

  export GP_JOB_MEMORY_MB="2000";
  assertEquals "custom memory" \
    "memory=2000," "$(mem_flag)"
    
  assertEquals "custom memory with arg" \
    "memory=4000," "$(mem_flag 4000)"
}

test_vcpus_flag() {
  assertIsFunctionDefined "vcpus_flag";
  assertEquals "by default, no vcpus flag" \
    "" "$(vcpus_flag)"
  export GP_JOB_CPU_COUNT="";
  assertEquals "ignore empty cpuCount" \
    "" "$(vcpus_flag)"
    
  export GP_JOB_CPU_COUNT="1";
  assertEquals "custom cpuCount=1" \
    "vcpus=1," "$(vcpus_flag)"
  export GP_JOB_CPU_COUNT="6";
  assertEquals "custom cpuCount=6" \
    "vcpus=6," "$(vcpus_flag)"

  export GP_JOB_CPU_COUNT="512";
  assertEquals "ignore custom cpuCount=512, exceeds max" \
    "" "$(vcpus_flag)"
}

# run the tests
source ${SHUNIT2_HOME}/src/shunit2
