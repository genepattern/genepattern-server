#!/usr/bin/env bash

#
# shunit2 test cases for wrapper script code
#

# called only once
oneTimeSetUp() {
  # strict_mode
  # Exit on error. Append || true if you expect an error.
  set -o errexit
  # Exit on error inside any functions or subshells.
  set -o errtrace
  # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
  set -o nounset
  # Catch the error in case mysqldump fails (but gzip succeeds) in `mysqldump |gzip`
  #   A trap on ERR, if set, is executed before the shell exits.
  set -o pipefail
  # Turn on traces, useful while debugging but commented out by default
  #set -o xtrace
  
  # disable exit on error for unit testing
  set +o errexit
}

# TODO: learn how to work with traps
#   see: https://unix.stackexchange.com/questions/208112/correct-behavior-of-exit-and-err-traps-when-using-set-eu
# Example,
# err_report() {
#   echo "error: (rc: $?)"
#   echo "error: on line $(caller)"
# }
#
# set_trap() {
#   trap 'err_report $LINENO' EXIT
# }


# Exploratory testing of bash parameter substitution 
#   see: http://www.tldp.org/LDP/abs/html/parameter-substitution.html
# Types of parameters to test
#   '1', first arg of function call, not set
#   notset, e.g. unset my_var;
#   empty, e.g. local my_var="";
#   nonempty, e.g. local my_var="my_value";
#
function test_parameter_substitution() {
  unset notset;
  local empty="";
  local param="value";
  
  echo "  \$# (num args): '$#'";
  echo "  notset, is not set";
  echo "  empty='$empty'";
  echo "  param='$param'";
  echo "";
  
  # ${parameter-default}, ${parameter:-default}, 
  #   if parameter not set, use default
  echo "\${parameter-default}, \${parameter:-default}"
  echo "  === if parameter not set, use default === "
  echo "  \${1-default}       : '${1-default}'";
  echo "  \${1:-default}      : '${1:-default}'";
  echo "  \${notset-default}  : '${notset-default}'";
  echo "  \${notset:-default} : '${notset:-default}'";
  echo "  \${empty-default}   : '${empty-default}'";
  echo "  \${empty:-default}  : '${empty:-default}'";
  echo "  \${param-default}   : '${param-default}'";
  echo "  \${param:-default}  : '${param:-default}'";
  echo "";

  # ${parameter=default}, ${parameter:=default},
  #   if parameter not set, set it to default
  #   note: can't set '$1'
  echo "\${parameter=default}, \${parameter:=default}"
  echo "  === if parameter not set, set it to default === ";
  unset notset;
  echo "  \${notset=default}  : '${notset=default}', after: notset='${notset}'";
  unset notset;
  echo "  \${notset:=default} : '${notset:=default}', after: notset='${notset}'";
  empty="";
  echo "  \${empty=default}   : '${empty=default}',        after: empty='${empty}'";
  empty="";
  echo "  \${empty:=default}  : '${empty:=default}', after: empty='${empty}'";
  param="value"
  echo "  \${param=default}   : '${param=default}',   after: param='${param}'";
  param="value"
  echo "  \${param:=default}  : '${param:=default}',   after: param=${param}";
  echo "";

  # ${parameter+alt_value}, ${parameter:+alt_value}                                                                                                                        
  #   If parameter set, use alt_value, else use null string.                                                                                                               
  unset notset;
  empty="";
  param="value";
  echo "\${parameter+alt_value}, \${parameter:+alt_value}";
  echo "  === If parameter set, use alt_value, else use null string. === ";
  echo "  \${1+alt_value}       : '${1+alt_value}';"
  echo "  \${1:+alt_value}      : '${1:+alt_value}';"
  echo "  \${notset+alt_value}  : '${notset+alt_value}';"
  echo "  \${notset:+alt_value} : '${notset:+alt_value}';"
  echo "  \${empty+alt_value}   : '${empty+alt_value}';"
  echo "  \${empty:+alt_value}  : '${empty:+alt_value}';"
  echo "  \${param+alt_value}   : '${param+alt_value}';"
  echo "  \${param:+alt_value}  : '${param:+alt_value}';"
}

# Is $1 an integer
function is_int() {
  # must have at least one arg
  if [ $# -eq 0 ]; then return 1; fi
    
  local arg;
  printf -v arg '%d\n' "${1:-x}" 2>/dev/null;
  # note: '$?' is non-zero when arg is not an integer
  #   return $?;
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

function test_is_int() {
  local arg;
  
  # valid arg1 tests, expecting true
  arg="-1";
  is_int "${arg}"  \
      || fail "    is_int '$arg', expecting true"
    arg="0";  
  is_int "${arg}"  \
      || fail "    is_int '$arg', expecting true"
  arg="8000";  
  is_int "${arg}"  \
      || fail "    is_int '$arg', expecting true"

  # invalid arg tests, expecting false
  is_int && fail "    is_int (no arg), expecting false"
  set +o nounset
  unset arg;
  is_int $arg && fail "    is_int (undefined arg), expecting false"
  set -o nounset
  local arg;

  # empty arg
  arg="";
  is_int  $arg \
      && fail "    is_int (empty string), expecting false"
  arg=" ";
  is_int "${arg}" \
      && fail "    is_int '$arg', expecting false"
  arg="AAA"; is_int "${arg}" \
      && fail "    is_int '$arg', expecting false"
  # decimal
  arg="0.1"; is_int "${arg}" \
        && fail "    is_int '$arg', expecting false"
}

function test_in_range() { 
  # invalid arg1 tests, expecting false
  # no arg
  in_range \
      && fail "    in_range (no arg), expecting false"

  # undefined arg
  set +o nounset
  unset arg;
  in_range  $arg \
    && fail "    in_range (undefined), expecting false"
  set -o nounset
    
  # empty arg
  local arg;
  arg="";
  in_range  $arg \
      && fail "    in_range (empty string), expecting false"
  arg=" ";
  in_range "${arg}" \
      && fail "    in_range '$arg', expecting false"
  arg="AAA"; in_range "${arg}" \
      && fail "    in_range '$arg', expecting false"
  # decimal
  arg="0.1"; in_range "${arg}" \
        && fail "    in_range '$arg', expecting false"

  # valid arg1 tests, expecting true
  arg="-1";
  in_range "${arg}"  \
      || fail "    in_range '$arg', expecting true"
    arg="0";  
  in_range "${arg}"  \
      || fail "    in_range '$arg', expecting true"
  arg="8000";  
  in_range "${arg}"  \
      || fail "    in_range '$arg', expecting true"

  #
  # Range tests
  #
  local min=;
  local max=;
  
  # positive int example
  arg="0";
  min="1";
  max="";
  # 0 is not positive
  in_range "${arg}" "${min}" "${max}" \
      && fail "    in_range '$arg' '$min' '$max', expecting false"
  # 1 is positive
  arg="1"; min="1"; max="";
  in_range "${arg}" "${min}" "${max}" \
        || fail "    in_range '$arg' '$min' '$max', expecting true"
  # 0 is not negative
  arg="0"; min=""; max="-1";
  in_range "${arg}" "${min}" "${max}" \
      && fail "    in_range '$arg' '$min' '$max', expecting false"
  # -1 is negative
  arg="-1"; min=""; max="-1";
  in_range "${arg}" "${min}" "${max}" \
      || fail "    in_range '$arg' '$min' '$max', expecting true"

  # range tests, expecting true
  min="400";
  max="99999";
  arg="8000";
  in_range "${arg}" "${min}" "${max}" \
      || fail "    in_range '$arg' '$min' '$max', expecting true"
  arg="400";
  in_range "${arg}" "${min}" "${max}" \
      || fail "    in_range '$arg' '$min' '$max', expecting true"
  arg="99999";
  in_range "${arg}" "${min}" "${max}" \
      || fail "    in_range '$arg' '$min' '$max', expecting true"
      
  # range tests, expecting false
  min="400";
  max="99999";
  arg="1";
  in_range "${arg}" "${min}" "${max}" \
      && fail "    in_range arg='$arg' min='$min' max='$max', expecting false"

  arg="100000";
  in_range "${arg}" "${min}" "${max}" \
      && fail "    in_range arg='$arg' min='$min' max='$max', expecting false"

  # range tests, ignore empty min range
  min="";
  max="99999";
  arg="8000";
  in_range "${arg}" "${min}" "${max}" \
      || fail "    in_range arg='$arg' min='$min' max='$max', ignore empty min range, expecting true"

  # range tests, invalid min range
  min="not a number";
  max="99999";
  arg="8000";
  in_range "${arg}" "${min}" "${max}" \
      && fail "    in_range arg='$arg' min='$min' max='$max', invalid min range, expecting false"

  # range tests, ignore empty max range
  min="400";
  max="";
  arg="8000";
  in_range "${arg}" "${min}" "${max}" \
      || fail "    in_range arg='$arg' min='$min' max='$max', ignore empty max range, expecting true"
  min="400";
  max="";
  arg="1";
  in_range "${arg}" "${min}" "${max}" \
      && fail "    in_range arg='$arg' min='$min' max='$max', ignore empty max range, expecting false"
      
}


source ${SHUNIT2_HOME}/src/shunit2
