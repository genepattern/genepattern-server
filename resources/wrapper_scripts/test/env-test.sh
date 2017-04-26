#!/usr/bin/env bash

#
# shunit2 test cases for wrapper script code
#


# called once before running any test
#   'source gp-common.sh'
oneTimeSetUp()  {
    readonly __test_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    readonly __parent_dir="$(cd "${__test_script_dir}/.."  && pwd)"
    readonly __gp_common_script="${__parent_dir}/gp-common.sh";
    
    # just for illustration, not presently used
    readonly __gp_script_file="${GP_SCRIPT_DIR}/$(basename "${BASH_SOURCE[0]}")";
    readonly __gp_script_base="$(basename ${__gp_script_file} .sh)";

    # for run-rjava tests
    run_r_path="../../../website/WEB-INF/classes/"
    
    # source 'gp-common.sh'
    if [ -f "${__gp_common_script}" ]; then
        source "${__gp_common_script}";
    else
        fail "gp-common-script not found: ${__gp_common_script}";
    fi
    strict_mode
    # disable exit on error for unit testing
    set +o errexit

}

# called before each test
setUp() {
    unset GP_ENV_CUSTOM
    unset GP_DEBUG
}

# called after each test
tearDown() {
    clearValues;
    unset GP_ENV_CUSTOM;
    unset GP_DEBUG
}

#
# low level, bash scripting specific test cases
#

# bash '-z' variable set test
#   [[ -z "${_my_var+x}" ]] is true when _my_var is not set
# Hint: using short-circuit instead of if-then statements
#   asserTrue:   <condition> || fail <message>
#   assertFalse: <condition> && fail <message>
test_bash_var_set() {
    unset _my_var;
    [[ -z "${_my_var+x}" ]] \
         || fail '(unbound) _my_var; [[ -z "${_my_var+x}" ]] (expecting true)';

    unset _my_var;
    local _my_var;
    [[ -z "${_my_var+x}" ]] \
         && fail 'local _my_var; [[ -z "${_my_var+x}" ]] (expecting false)';

    unset _my_var;
    local _my_var="";
    [[ -z "${_my_var+x}" ]] \
         && fail 'local _my_var=""; [[ -z "${_my_var+x}" ]] (expecting false)';

    unset _my_var;
    local _my_var="_my_value";
    [[ -z "${_my_var+x}" ]] \
           && fail 'local _my_var="_my_value"; [[ -z "${_my_var+x}" ]] (expecting false)';
        unset _my_var;
}

# basic '-e' file exists test
test_bash_file_exists() {
    assertTrue "fileExists('env-test.sh')" \
      "[ -e 'env-test.sh' ]"

    prefix="env-";
    suffix="test.sh";
    assertTrue "fileExists('$prefix$suffix')" \
      "[ -e $prefix$suffix ]"
}

join() {
    local IFS=$1;
    shift;
    echo "$*";
}

function test_bash_join_array() {
    declare -a myArray=('arg1' 'arg2');
    assertEquals "join, no delim" "arg1 arg2" "$(join ' ' ${myArray[@]})"
    assertEquals "join, custom delim" "arg1,arg2" "$(join ',' ${myArray[@]})"
}

# Split comma-delimited string into args
# use-case: R requires a particular GCC version
# Example code:
#   IFS=', ' read -a array <<< "$string", oldIFS="$IFS", ..., IFS="$oldIFS"

test_bash_split_args() {
    putValue "R-2.15" "GCC-4.9, R-2.15" 
    assertEquals "__indexOf('R-2.15')" "0" $(__indexOf 'R-2.15')
    assertEquals "getValue('R-2.15')" "GCC-4.9, R-2.15" "$(getValue 'R-2.15')"

    # split into values
    value="$(getValue 'R-2.15')"
    IFS=', ' read -a valueArray <<< "$value"
    assertEquals "valueArray[0]" "GCC-4.9" "${valueArray[0]}"
    assertEquals "valueArray[1]" "R-2.15" "${valueArray[1]}"
}

#
# test parsing of "key=val" from a string
#
test_bash_split_key_value() {
    local input="MY_ARG=MY_VAL"
    IFS='=' read -r -a args <<< "$input"
    assertTrue "$input, hasEquals" "[[ $input == *=* ]]"
    assertEquals "$input, args.length", "2" "${#args[@]}"
    assertEquals "$input, args[0]" "MY_ARG" "${args[0]}"
    assertEquals "$input, args[1]" "MY_VAL" "${args[1]}"
    
    input="MY_ARG"
    IFS='=' read -r -a args <<< "$input"
    assertTrue "$input, not hasEquals" "! [[ $input == *=* ]]"
    assertEquals "$input, args.length", "1" "${#args[@]}"
    assertEquals "$input, args[0]" "MY_ARG" "${args[0]}"

    input="MY_ARG="
    IFS='=' read -r -a args <<< "$input"
    assertTrue "$input, hasEquals" "[[ $input == *=* ]]"
    assertEquals "$input, args.length", "1" "${#args[@]}"
    assertEquals "$input, args[0]" "MY_ARG" "${args[0]}"
    
    input="=MY_VAL"
    IFS='=' read -r -a args <<< "$input"
    assertTrue "$input, hasEquals" "[[ $input == *=* ]]"
    assertEquals "$input, args.length", "2" "${#args[@]}"
    assertEquals "$input, args[0]" "" "${args[0]}"
    assertEquals "$input, args[1]" "MY_VAL" "${args[1]}" 
}


############################################################
# test gp-common functions
############################################################

############################################################
# for debugging, gp::sourceDir, 
# get the fully qualified directory path of the current 
# bash script. This is a workaround for Mac OS X error
#   'readlink: illegal option -- f'
#
# Usage:
#   gp::source_dir [<filename>]
# Arguments:
#   filename, optional, default=${BASH_SOURCE[0]} 
#
# Note: this command may not work as expected with 
# symbolic links.
############################################################
function gp::source_dir() { 
    local __arg="${1:-BASH_SOURCE[0]}";
    echo "$( cd "$( dirname "${__arg}" )" && pwd )";
}

# test GP_SCRIPT_DIR 
test_gp_script_dir() {
  assertEquals "GP_SCRIPT_DIR" "${__parent_dir}" \
    "${GP_SCRIPT_DIR:-}";

  # the remaining tests are for debugging,
  #     they may not be essential for production 
  assertEquals "gp::source_dir (no args)" "${__test_script_dir}" \
    "$(gp::source_dir)";
  assertEquals "gp::source_dir (relative arg)" "${__test_script_dir}" \
    "$(gp::source_dir 'env-test.sh')";
  assertEquals "gp::source_dir (fq arg)" "${__parent_dir}" \
    "$(gp::source_dir "${__gp_common_script}")";

  # special-case: Mac OS X error
  #     'readlink: illegal option -- f'
  # skip the test, keeping it for future reference
  startSkipping
  if ! isSkipping; then
    assertEquals "default (no args), with readlink"  \
      "${__test_script_dir}" \
      "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")";
  fi
  endSkipping;
  
  ## for R script rewrite
  local __local_parent_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd ../ && pwd)"
  assertEquals "parent_dir check" "${__parent_dir}" "${__local_parent_dir}"
}

# test gp-common::export_env
test_export_env() {
    assertEquals "export_env sanity check (no arg)" "" \
      "$(export_env)"
    assertEquals "export_env sanity check (no left hand value)" "" \
      "$(export_env '=MY_VAL')"
        
    unset MY_KEY
    assertTrue "MY_KEY unset" "[ -z ${MY_KEY+x} ]"
    
    local input="MY_KEY=MY_VAL"
    export_env "${input}"
    assertTrue   "export_env '$input'; MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
    assertEquals "export_env '$input'; \$MY_KEY" "MY_VAL" "$MY_KEY"
    
    # export_env 'MY_KEY=' should unset the value
    input='MY_KEY='
    export_env "${input}"
    assertTrue "export_env 'MY_KEY='; should unset the value" "[ -z ${MY_KEY+x} ]"
    
    # export_env 'MY_KEY' (no equals sign) should set an empty value
    input="MY_KEY"
    export_env "${input}"
    assertTrue   "export_env '$input'; MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
    assertEquals "export_env '$input'; \$MY_KEY (empty)" "" "$MY_KEY"
}

# test gp-common::set_env_custom_script()
test_set_env_custom_script() {
    # test-case
    local readonly _fq_path="/opt/gp/my custom with spaces.sh";
    unset GP_ENV_CUSTOM;
    set_env_custom_script
    assertEquals "default" \
        "${__parent_dir}/env-custom.sh" \
        "${__gp_env_custom_script}"
    # test-case
    set_env_custom_script ''
    assertEquals "empty string" \
        "${__parent_dir}/env-custom.sh" \
        "${__gp_env_custom_script}"
    # test-case
    set_env_custom_script 'env-custom-macos.sh'
    assertEquals "custom -c arg, relative" \
        "${__parent_dir}/env-custom-macos.sh" \
        "${__gp_env_custom_script}"
    # test-case
    set_env_custom_script "${_fq_path}"
    assertEquals "custom -c arg, fully qualified" \
        "${_fq_path}" \
        "${__gp_env_custom_script}"
    # test-case
    GP_ENV_CUSTOM="env-custom-macos.sh";
    set_env_custom_script
    assertEquals "export GP_ENV_CUSTOM, relative" \
        "${__parent_dir}/env-custom-macos.sh" \
        "${__gp_env_custom_script}";
    # test-case
    GP_ENV_CUSTOM="${_fq_path}";
    set_env_custom_script
    assertEquals "export GP_ENV_CUSTOM, relative" \
        "${_fq_path}" \
        "${__gp_env_custom_script}";
        unset GP_ENV_CUSTOM;
}

# test gp-common::convertPath()
test_convertPath() {
    assertEquals "no arg" "${GP_SCRIPT_DIR}/" \
        $(convertPath)
    assertEquals "relative path" "${GP_SCRIPT_DIR}/env-custom.sh" \
        $(convertPath 'env-custom.sh')
    assertEquals "fq path to dir" "/opt/genepattern" \
        $(convertPath '/opt/genepattern')
    assertEquals "fq path to dir, trailing slash" "/opt/genepattern/" \
        $(convertPath '/opt/genepattern/')
    assertEquals "fq path to file" "/opt/genepattern/test.sh" \
        $(convertPath '/opt/genepattern/test.sh') 
}

# test gp-common::extractRootName()
test_rootModuleName() {
    # standard use-case
    moduleName="R/2.15.3"
    assertEquals "rootName('$moduleName')" "R" "$(extractRootName $moduleName)"
    
    # case 2: extra sub-package
    moduleName="R/2.15/3"
    assertEquals "rootName('$moduleName')" "R" "$(extractRootName $moduleName)"
    
    # case 3: no sub-package
    moduleName="R"
    assertEquals "rootName('$moduleName')" "R" "$(extractRootName $moduleName)"

    # case 4: empty string
    moduleName=""
    assertEquals "rootName('$moduleName')" "" "$(extractRootName $moduleName)"
}

# test gp-common::echoEnv
test_echoEnv() {
    putValue "java/1.8"
    putValue "python/2.5"
    echoEnv
}

############################################################
# test gp-common:parse_args
############################################################

# test parse_args() with '-c' <env-custom> arg
test_parse_args_c_flag() {
  parse_args '-c' 'env-custom-macos.sh' 'echo' 'Hello, World!';
  assertEquals "__gp_env_custom_arg" \
    "env-custom-macos.sh" "${__gp_env_custom_arg}"
  assertEquals "__gp_env_custom_script" \
    "${GP_SCRIPT_DIR}/env-custom-macos.sh" "${__gp_env_custom_script}"
  assertEquals "__gp_module_cmd" \
    "echo Hello, World!" \
    "${__gp_module_cmd[*]:0}"
}

# test parse_args() with '-c', special-case
#   missing arg with other options
test_parse_args_c_flag_missing_arg() {
  parse_args '-c' '-u' 'java/1.8' 'java' '--version';
  assertEquals "'-c' arg" "" "${__gp_env_custom_arg}"
  assertEquals "env_custom" "${GP_SCRIPT_DIR}/env-custom.sh" \
    "${__gp_env_custom_script}"
  assertEquals "__gp_module_cmd" \
    "java --version" \
    "${__gp_module_cmd[*]:0}";
}

# test parse_args() with '-c', special-case
#   missing arg at end of options
test_parse_args_c_flag_missing_arg_at_end() {

  # note: must add '--' between run-with-env args and module command, e.g.
  #   this fails: parse_args '-c' 'echo' 'Hello, World!'
  #   this works: parse_args '-c' '--' 'echo' 'Hello, World!'
  parse_args '-c' '--' 'echo' 'Hello, World!'
  assertEquals "'-c' arg" "" "${__gp_env_custom_arg}"
  assertEquals "env_custom" "${GP_SCRIPT_DIR}/env-custom.sh" \
    "${__gp_env_custom_script}"
  assertEquals "__gp_module_cmd" "echo Hello, World!" \
    "${__gp_module_cmd[*]:0}"
}

# test parse_args, with '-e' GP_ENV_CUSTOM=<env-custom>
test_parse_args_e_flag_set_env_custom() {
  unset GP_ENV_CUSTOM;
  parse_args "-e" "GP_ENV_CUSTOM=env-custom-macos.sh" "--" "echo" "Hello, World!";
  assertEquals "GP_ENV_CUSTOM" "env-custom-macos.sh" \
    "${GP_ENV_CUSTOM:-}";
  assertEquals "__gp_env_custom_arg" "" \
    "${__gp_env_custom_arg}"
  assertEquals "__gp_env_custom_script" "${GP_SCRIPT_DIR}/env-custom-macos.sh" \
    "${__gp_env_custom_script}"
  assertEquals "__gp_module_cmd" "echo Hello, World!" \
    "${__gp_module_cmd[*]:0}"
}

# test parse_args, with export GP_ENV_CUSTOM=<env-custom>
test_parse_args_GP_ENV_CUSTOM() {
  export GP_ENV_CUSTOM="env-custom-macos.sh";
  parse_args "echo" "Hello, World!";
  assertEquals "__gp_env_custom_arg" "" "${__gp_env_custom_arg}"
  assertEquals "__gp_env_custom_script" "${GP_SCRIPT_DIR}/env-custom-macos.sh" "${__gp_env_custom_script}"
  assertEquals "__gp_module_cmd" \
    "echo Hello, World!" \
    "${__gp_module_cmd[*]:0}"
}

# test parse_args, '-e' 'KEY=VAL'
test_parse_args_e_flag() {
    local -a args=('-e' 'MY_KEY=MY_VAL' '-e' 'MY_KEY2=MY_VAL2' 'echo' 'Hello, World!');
    parse_args "${args[@]}";

    # expecting two -e args
    assertEquals "num '-e' args" "2" "${#__gp_e_args[@]}"
    assertEquals "__gp_e_args" "MY_KEY=MY_VAL MY_KEY2=MY_VAL2" "${__gp_e_args[*]:-}"
}

# test parse_args, '-u' canonical-name
test_parse_args_u_flag() {
    assertEquals "parse_args -u Java" \
      "Java" \
      "$(parse_args "-u" "Java" && echo "${__gp_u_args[*]}")"

    assertEquals "parse_args -u java/1.8" \
      "java/1.8" \
      "$(parse_args "-u" 'java/1.8' && echo "${__gp_u_args[*]}")"

    assertEquals "parse_args -u java/1.8 -u gcc/4.9" \
      "java/1.8 gcc/4.9" \
      "$(parse_args "-u" 'java/1.8' '-u' 'gcc/4.9' && echo "${__gp_u_args[*]}")"

    assertEquals "parse_args -u 'Java 1.8'" \
      "Java 1.8" \
      "$(parse_args "-u" "Java 1.8" && echo "${__gp_u_args[*]}")"
}

# test parse_args, example java command
test_parse_args_java_cmd() {
    local -a args=('-u' 'Java-1.8' 'java' '--version');
    parse_args "${args[@]}";

    assertEquals "env_default" "${GP_SCRIPT_DIR}/env-default.sh" \
        "${__gp_env_default_script}"
    assertEquals "'-c' arg" "" "${__gp_env_custom_arg}"
    assertEquals "env_custom" "${GP_SCRIPT_DIR}/env-custom.sh" \
        "${__gp_env_custom_script}"

    # check requested environment variables
    assertEquals "num '-e' args" "0" "${#__gp_e_args[@]}"
    # check requested modules
    assertEquals "num '-u' args" "1" "${#__gp_u_args[@]}"
    # check command line
    assertEquals "__gp_module_cmd" \
        'java --version' \
        "${__gp_module_cmd[*]}"
}

# test parse_args, sanity check with no args
test_parse_args_no_args() {
    parse_args
    assertEquals "env_default" "${GP_SCRIPT_DIR}/env-default.sh" \
        "${__gp_env_default_script}"
    assertEquals "'-c' arg" "" "${__gp_env_custom_arg}"
    assertEquals "env_custom" "${GP_SCRIPT_DIR}/env-custom.sh" \
        "${__gp_env_custom_script}"

    assertEquals "num '-e' args" "0" "${#__gp_e_args[@]}"
    assertEquals "num '-u' args" "0" "${#__gp_u_args[@]}"
    assertEquals "num __gp_module_cmd args" \
         "0" \
        "${#__gp_module_cmd[@]}"
    assertEquals "__gp_module_cmd" \
         "" \
        "${__gp_module_cmd[*]:-}"
}

#
# double-check the syntax of the initEnv function in the env-custom-macos.sh script
# These tests run on non-Mac systems; just validate that the bash syntax is correct
# and that the PATH is set to the expected R Library location
#
# Note: The existing library does not remove an item from the PATH; make sure that the
#     test cleans up after itself
#
test_env_custom_macos() {
    source "../env-custom-macos.sh"
    local PATH_ORIG="${PATH}"
    
    # test 1: R-2.0
    initEnv R-2.0
    assertEquals "R-2.0 PATH" "/Library/Frameworks/R.framework/Versions/2.0/Resources/bin:${PATH_ORIG}" "${PATH}";
    export PATH=${PATH_ORIG}

    # test 2: R-2.5
    initEnv R-2.5
    assertEquals "R-2.5 PATH" "/Library/Frameworks/R.framework/Versions/2.5/Resources/bin:${PATH_ORIG}" "${PATH}";
    export PATH=${PATH_ORIG} 
    
    # test 3: r/2.5 canonical name
    putValue 'r/2.5' 'R-2.5'
    initEnv 'r/2.5'
    assertEquals "r/2.5 PATH" "/Library/Frameworks/R.framework/Versions/2.5/Resources/bin:${PATH_ORIG}" "${PATH}";
    export PATH=${PATH_ORIG} 
}

#
# basic stress-testing of the env-hashmap.sh script
#
#
test_num_args() {
    assertEquals "__num_args" "0" "$(__num_args)"
    assertEquals "__num_args" "4" "$(__num_args this is a test)"
    declare -a my_arr=("this" "is" "a" "test");
    assertEquals "__num_args my_arr" "4" "$(__num_args "${my_arr[@]}")"
    my_arr=("this is" "a" "test");
    assertEquals "__num_args with spaces" "3" "$(__num_args "${my_arr[@]}")"
}

test_env_hashmap_init() {
    # initial
    assertEquals "numKeys initial" \
        "0" $(numKeys)
    assertEquals "numEnvs initial" \
        "0" $(numEnvs)
    assertTrue   "numKeys -eq 0 initial" \
        "[ 0 -eq $(numKeys) ]"
    assertTrue   "[ numEnvs -eq 0 ] initial" \
        "[ 0 -eq $(numEnvs) ]"
    assertTrue   "isEmpty initial" \
        "[ isEmpty ]"
    assertTrue   "isEmptyEnv initial" \
        "[ isEmptyEnv ]"

    # putValue (1st)
    putValue "key_01" "value_01"
    assertEquals "numKeys, 1st" \
        "1" $(numKeys)
    assertTrue   "[ 1 -eq numKeys ], 1st" \
        "[ 1 -eq $(numKeys) ]"

    # putValue (2nd)
    putValue "key_02" "value_02"
    assertEquals "numKeys, 2nd" \
        "2" "$(numKeys)"
    assertTrue   "[ 2 -eq numKeys ], 2nd" \
        "[ 2 -eq $(numKeys) ]"
            
}

test_env_hashmap() {
    putValue "A" "a"
    putValue "B" "b"
    putValue "C" "c"
    
    assertTrue "hasIndex('A')"  "[ $(__indexOf 'A') -gt -1 ]"
    assertTrue "hasIndex('B')"  "[ $(__indexOf 'B') -gt -1 ]"
    assertTrue "hasIndex('C')"  "[ $(__indexOf 'C') -gt -1 ]"
    assertTrue "not hasIndex('D')"  "! [ $(__indexOf 'D') -gt -1 ]"
    assertEquals "getValue('A')" "a" $(getValue 'A')
    assertEquals "getValue('B')" "b" $(getValue 'B')
    assertEquals "getValue('C')" "c" $(getValue 'C')

    # sourcing env-hashmap.sh a second time should not clear the hash map 
    source ../env-hashmap.sh
    assertEquals "after source env-hashmap.sh a 2nd time, getValue('C')" "c" $(getValue 'C')

    # clearValues does reset the map
    clearValues
    assertTrue "after clearValues, not hasIndex('C')"  "! [ $(__indexOf 'C') -gt -1 ]"
    assertEquals "after clearValues, getValue('C')" "C" $(getValue 'C')
}

# when the key is not in the map, return the key
test_putValue_no_arg() {
    putValue 'my_key'
    assertEquals "getValue('my_key')" "my_key" "$(getValue 'my_key')"
}

test_putValue_with_spaces() {
    assertTrue "numKeys -eq 0, initial" "[ 0 -eq $(numKeys) ]"
    assertTrue "isEmpty (initial)" "[ isEmpty ]"
    
    putValue "A" "a" 
    putValue "B" "a space" 
    assertEquals "__indexOf('B')" "1" $(__indexOf 'B')
    assertEquals "getValue('B')" "a space" "$(getValue 'B')"
}

test_putValue_with_delims() {
    putValue "A" "a" 
    putValue "B" "val1, val2" 
    assertEquals "__indexOf('B')" "1" $(__indexOf 'B')
    assertEquals "getValue('B')" "val1, val2" "$(getValue 'B')"
}

test_putKey_with_spaces() {
    putValue "A" "a"
    putValue "B KEY" "b"
    assertEquals "__indexOf('B KEY')" "1" $(__indexOf 'B KEY')
    assertEquals "getValue('B KEY')" "b" "$(getValue 'B KEY')"
}

# when the key is not in the map, return the key
test_getValue_not_set() {
    local key="Java-1.7";
    assertEquals "__indexOf($key)" "-1" $(__indexOf $key)
    assertEquals "getValue($key)" "$key" $(getValue $key)
}

# when the key is one of the canonical keys and there is no customization, 
#   return the value
test_getValue_env_default() {
    assertTrue "__indexOf('Java-1.7') before sourceEnvDefault" "[ "-1" -eq "$(__indexOf 'Java-1.7')" ]"
    source ../env-default.sh
    assertTrue "__indexOf('Java-1.7')" "[ "-1" -ne "$(__indexOf 'Java-1.7')" ]"
    
    assertEquals "'Java' set in env-default.sh" \
      "Java-1.7" "$(getValue 'Java')"
    assertEquals "'java' set in env-default.sh" \
      "Java-1.7" "$(getValue 'java')"
    assertEquals "'Java-1.7' set in env-default.sh" \
      "Java-1.7" "$(getValue 'Java-1.7')"
}

# test-cases for getValue with site-customization script 
#   see: env-custom-shunit.sh
test_getValue_env_custom() {
    source "../env-default.sh";
    source "${__test_script_dir}/env-custom-shunit.sh"
    
    # 'my-env' not set in env-default or env-custom
    assertEquals "'my-env' not set" \
      "my-env" "$(getValue 'my-env')"
    # custom 'Java'
    assertEquals "custom Java version" \
      "java/1.8" "$(getValue 'Java')"
    # transitive dependencies ...
    assertEquals "'java' transitive dependency" \
      "java/1.8" "$(getValue 'java')"
    assertEquals "'Java-1.8' transitive dependency" \
      "java/1.8" "$(getValue 'Java-1.8')"
    # custom 'matlab'
    assertEquals "custom value for 'matlab-mcr/2014a'" \
      ".matlab_2014a_mcr" "$(getValue 'matlab-mcr/2014a')"
    # map one key to multiple values
    assertEquals "map one key to multiple values, 'R-3.1' requires 'GCC-4.9'" \
      "GCC-4.9, R-3.1" "$(getValue R-3.1)"
    # map key with aliases
    assertEquals "dependency with aliases, 'R-3.0' requires 'GCC-4.7.2'" \
      "gcc/4.7.2, R/3.0.1" "$(getValue 'R-3.0')"
}

test_addEnv_env_custom() {
  source "../env-default.sh";
  source "${__test_script_dir}/env-custom-shunit.sh"
  # local counter to make it easier to add test-cases, hint: ((idx++)
  local -i idx=-1;
  local my_env=;
  
  # 'my-env' not set
  addEnv 'my-env'; ((idx++));
  assertEquals "addEnv 'my-env'" \
    "my-env" \
    "${__gp_module_envs[$idx]}"
 
  # custom Java
  addEnv "Java"; ((idx++));
  assertEquals "addEnv 'Java'" \
    "java/1.8" \
    "${__gp_module_envs[$idx]}"
    
  # transitive dependency
  addEnv "Java-1.8"; ((idx++));
  assertEquals "addEnv 'Java-1.8'" \
    "java/1.8" \
    "${__gp_module_envs[$idx]}"

  # custom 'matlab'
  addEnv "matlab-mcr/2014a"; ((idx++));
  assertEquals "addEnv 'matlab-mcr/2014a'" \
    ".matlab_2014a_mcr" \
    "${__gp_module_envs[$idx]}"

  # set dependency, R-3.1 depends on gcc
  addEnv "R-3.1"; ((idx++));
  assertEquals "addEnv 'R-3.1[0]'" \
    "GCC-4.9" \
    "${__gp_module_envs[$idx]}"
  ((idx++));
  assertEquals "addEnv 'R-3.1[1]'" \
    "R-3.1" \
    "${__gp_module_envs[$idx]}"

  # set dependency with aliases, R-3.0 depends on gcc, with custom names
  addEnv "R-3.0"; ((idx++));
  assertEquals "addEnv 'R-3.0[0]'" \
    "gcc/4.7.2" \
    "${__gp_module_envs[$idx]}"
  ((idx++));
  assertEquals "addEnv 'R-3.0[1]'" \
    "R/3.0.1" \
    "${__gp_module_envs[$idx]}"

  # expecting idx+1 entries
  assertEquals "__gp_module_envs.size" "$((idx+1))" "${#__gp_module_envs[@]}"
}

# test-case, initEnv, export RGL_USE_NULL in site-customization script
test_initEnv_set_RGL_USE_NULL() {
    source "../env-default.sh";
    source "${__test_script_dir}/env-custom-shunit.sh"
    
    assertTrue   "sanity check before test, RGL_USE_NULL must not be set" \
      "[ -z ${RGL_USE_NULL+x} ]"
    assertEquals "sanity check before test, RGL_USE_NULL" \
      "(not set)" "$(echo ${RGL_USE_NULL:-(not set)})"
    initEnv ".libmesa_from_matlab-2014b"
    assertEquals "after initEnv '.libmesa_from_matlab-2014b', RGL_USE_NULL" \
      "TRUE" "$(echo $RGL_USE_NULL)"
}

# test 'run-rscript' substitution
#   run-rscript.sh -c <env-custom> -a <env-arch> -l <libdir> -p <patches> -v 2.15 --
# Variations
#     # original production version circa gp/3.9.9
#     -c <env-custom> -l <libdir> -p <patches>
#     # customized on gpprod for 'env-arch' flag
#     -c <env-custom> -l <libdir> -p <patches> -a <env-arch>
#     # [not implemented] set as -e environment variables
#     -e GP_ENV_CUSTOM=<env-custom> -e GP_ENV_ARCH=<env-arch> -l <libdir> -p <patches>
#
# R2.15_Rscript=
#   <run-rscript> -v 2.15 --
#   run-rscript.sh -c <env-custom> -a <env-arch> -l <libdir> -p <patches> -v 2.15 --
test_run_rscript() { 
  local script_dir=$( cd ../ && pwd )    
  local patches="patches";
  local libdir="taskLib/ConvertLineEndings.2.1"
  export GP_DEBUG="false"

  # expected '-n', dry_run output
  local expected="";
  # template to be modified before assertions
  local expected_template="${script_dir}/run-with-env.sh EXPECTED_ENV_CUSTOM \
-u R-2.15 -e GP_DEBUG=FALSE -e R_LIBS= -e R_LIBS_USER=' ' \
-e R_LIBS_SITE=${patches}/EXPECTED_ENV_ARCHLibrary/R/2.15 \
-e R_ENVIRON=${script_dir}/R/Renviron.gp.site \
-e R_ENVIRON_USER=${script_dir}/R/2.15/Renviron.gp.site \
-e R_PROFILE=${script_dir}/R/2.15/Rprofile.gp.site \
-e R_PROFILE_USER=${script_dir}/R/2.15/Rprofile.gp.custom \
Rscript --version";

  # test-case: -c env-custom-centos5.sh -a centos5
  expected="${expected_template/EXPECTED_ENV_CUSTOM/-c env-custom-centos5.sh}";
  expected="${expected/EXPECTED_ENV_ARCH/centos5/}";
  assertEquals "run-rscript, -c <env-custom> -a <env-arch>" \
    "${expected}" \
    "$(../run-rscript.sh -n -c env-custom-centos5.sh -l ${libdir} -p ${patches} -a centos5 -v 2.15 -- --version)"

  # test-case: no '-c' option, no '-a' option
  expected="${expected_template/EXPECTED_ENV_CUSTOM }";
  expected="${expected/EXPECTED_ENV_ARCH}";
  assertEquals "run-rscript, no '-c' option, no '-a' option" \
    "${expected}" \
    "$(../run-rscript.sh -n -l ${libdir} -p ${patches} -v 2.15 -- --version)"
    
  # test-case: -c <env-custom>, no '-a' option
  expected="${expected_template/EXPECTED_ENV_CUSTOM/-c env-custom-macos.sh}";
  expected="${expected/EXPECTED_ENV_ARCH}";
  assertEquals "run-rscript, -c <env-custom>, no '-a' option" \
    "${expected}" \
    "$(../run-rscript.sh -n -c env-custom-macos.sh -l ${libdir} -p ${patches} -v 2.15 -- --version)"

  # test-case: '-a' missing arg
  expected="${expected_template/EXPECTED_ENV_CUSTOM/-c env-custom-macos.sh}";
  expected="${expected/EXPECTED_ENV_ARCH}";
  assertEquals "run-rscript, '-a' missing arg" \
    "${expected}" \
    "$(../run-rscript.sh -n -c env-custom-macos.sh -l ${libdir} -p ${patches} -a -v 2.15 -- --version)"

  # test-case: '-a' empty arg
  expected="${expected_template/EXPECTED_ENV_CUSTOM/-c env-custom-macos.sh}";
  expected="${expected/EXPECTED_ENV_ARCH}";
  assertEquals "run-rscript, '-a' empty arg" \
    "${expected}" \
    "$(../run-rscript.sh -n -c env-custom-macos.sh -l ${libdir} -p ${patches} -a '' -v 2.15 -- --version)"
}

# test 'run-rjava' substitution
#   run-rjava=<wrapper-scripts>/run-rjava.sh -c <env-custom>
# Variations
#   R2.5_Rjava=<run-rjava> 2.5 <rjava_flags> -cp <run_r_path> RunR
#   rjava_flags=-Xmx512m
#   run_r_path=<webappDir>/WEB-INF/classes
test_run_rjava() {
  export GP_DEBUG="true";
  local expected=$'loading R-2.5 ...\nloading Java-1.7 ...'
  assertEquals "run R2.5" \
    "$expected" \
    "$('../run-rjava.sh' '2.5' '-version')"
}

# test run-rjava.sh with '-c' arg
test_run_rjava_env_custom_arg() {
    export GP_DEBUG="true";
    local expected=$'loading r-2.5-vanilla-gp ...\nloading java/1.8 ...'
    assertEquals "run R2.5" \
      "$expected" \
      "$('../run-rjava.sh' '-c' "${__test_script_dir}/env-custom-shunit.sh" '2.5' '-version')"
}

# test <run-rjava> substitution with hello.R input
# note: this is a full integration test which invokes R 
#   via the java RunR wrapper command
#   this test may fail on systems where R is not configured
#   it is ok to skip this test if necessary 
test_run_rjava_hello() {
  #local run_r_path="../../../website/WEB-INF/classes/"

  # test-case: default site-customization    
  local out="$(../run-rjava.sh 2.5 -Xmx512m -cp ${run_r_path} RunR hello.R hello)";
  assertTrue "run-rjava, output ends with 'Hello, world!'" \
    "[[ \"${out}\" = *\"Hello, world!\" ]]"
  
  # test-case: -c env-custom-macos.sh
  out="$(../run-rjava.sh \
    -c env-custom-macos.sh 2.5 \
    -Xmx512m -cp ${run_r_path} RunR hello.R hello)";
  assertTrue "run-rjava with '-c', output ends with 'Hello, world!'" \
    "[[ \"${out}\" = *\"Hello, world!\" ]]"
}

# test <run-rjava> alternative, see above
test_run_rjava_with_env_hello() {
  #local run_r_path="../../../website/WEB-INF/classes/"
  out="$(../run-rjava-with-env.sh \
    -c env-custom-macos.sh -u java/1.8 -u r/2.5 -- \
    -Xmx128m -cp ${run_r_path} RunR hello.R hello)";
  # debug: echo $out
  assertTrue "run-rjava-with-env output ends with 'Hello, world!'" \
    "[[ \"${out}\" = *\"Hello, world!\" ]]"
}

test_run_with_env() {
  export GP_DEBUG="true"
  local expected=$'loading Java-1.7 ...\nHello, World!'
  assertEquals "run-with-env, with GP_DEBUG" \
    "${expected}" \
    "$(../run-with-env.sh -u Java echo 'Hello, World!')"

  unset GP_DEBUG
  assertEquals "run-with-env" \
    "Hello, World!" \
    "$(../run-with-env.sh -u Java echo 'Hello, World!')"    
}

test_run_with_env_custom_env_arg() {
  export GP_DEBUG="true";
  local expected=$'loading GCC-4.9 ...\nloading R-3.1 ...\nHello, World!';
  assertEquals "run-with-env -c env-custom-shunit.sh, with GP_DEBUG" \
    "$expected" \
    "$(../run-with-env.sh -c "${__test_script_dir}/env-custom-shunit.sh" -u R-3.1 echo 'Hello, World!')"
}

# set environment variable with '-e' option
#   <run-with-env> ... -e <env.key=env.value> ... 
test_run_with_env_setenv_on_cmdLine() {
  local cmd="${__test_script_dir}/print-my-flag.sh"  
  assertEquals "print-my-flag.sh (no args)" \
    "MY_FLAG=" \
    "$( $cmd )"
  assertEquals "run-with-env ... -e MY_FLAG=MY_CMD_LINE_VALUE ... print-my-flag.sh" \
    "MY_FLAG=MY_CMD_LINE_VALUE" \
    "$( '../run-with-env.sh' \
          '-e' 'MY_UNUSED_FLAG'  \
          '-e' '=BOGUS_VALUE' \
          '-e' 'MY_UNSET_FLAG=' \
          '-e' 'MY_EMPTY_FLAG' \
          '-e' 'MY_FLAG=MY_CMD_LINE_VALUE' \
          '-u' 'Java-1.7' \
          $cmd )"
}

# test-case: <run-with-env> with invalid command line option
#   expecting the script to exit early rather than go into an infinite loop
test_run_with_env_invalid_option() {
    unset GP_DEBUG
    assertEquals "invalid '-Xmx512m' command line arg" \
      "../run-with-env.sh: illegal option -- X" \
      "$((../run-with-env.sh -Xmx512m -u Java echo 'Hello, World!') 2>&1)"
}

test_run_java_default() {
  export GP_DEBUG="true";
  assertEquals "run-java.sh --version" \
    "loading Java-1.7 ..." \
    "$('../run-java.sh' '--' '-version')"
}

test_run_java_custom_env() {
  export GP_DEBUG="true";
  assertEquals "run-java.sh custom_env" \
    "loading java/1.8 ..." \
    "$('../run-java.sh' '-c' './test/env-custom-shunit.sh' '--' '-version')"
}

# Prepend an element to the beginning of the path; 
# Usage: 
#   path=$(prepend_path "${element}" "${path}")
function prepend_path() {
    local element="${1}";
    local path="${2}";
    
    # Note, to check for a directory: [ -d "$element" ] 
    # To prepend, path="$element:$path"
    
    # if path is not set ... just set it to element
    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
    if [[ -z "$path" ]]; then
        #echo "2, path not set";
        path="$element";
    elif [[ ":$path:" != *":$element:"* ]]; then
        path="$element:$path";
    fi
    # use echo to return a value
    echo "$path"
}

# Append an element to the end of the path; 
# Usage: 
#   path=$(append_path "${path}" "${element}")
function append_path() {
    local path="${1}";
    local element="${2}";
    
    # Note, to check for a directory: [ -d "$element" ] 
    # To prepend, path="$element:$path"
    
    # if path is not set ... just set it to element
    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
    if [ -z "$path" ]; then
        #debug: echo "2, path not set";
        path="$element";
    elif [[ ":$path:" != *":$element:"* ]]; then
        path="${path:+"$path:"}$element"
    fi
    # use echo to return a value
    echo "$path"
}

test_append_path() {
    MY_PATH="/opt/dir1";
    MY_PATH=$(append_path "${MY_PATH}" "/opt/dir2")
    assertEquals "append_path" "/opt/dir1:/opt/dir2" "${MY_PATH}"
}

test_append_path_ignoreDuplicate() {
    MY_PATH="/opt/dir1:/opt/dir2:/opt/dir3";
    
    assertEquals "append_path, ignore dupe in front" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(append_path "${MY_PATH}" "/opt/dir1");
    
    assertEquals "append_path, ignore dupe in middle" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(append_path "${MY_PATH}" "/opt/dir2");
    
    assertEquals "append_path, ignore dupe at end" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(append_path "${MY_PATH}" "/opt/dir3");
        
    # sanity check
    assertEquals "sanity check, not a dupe" "/opt/dir1:/opt/dir2:/opt/dir3:/opt/dir4" \
        $(append_path "${MY_PATH}" "/opt/dir4"); 
}

test_append_path_toEmpty() {
    MY_ARG="/new/pathelement";
    unset MY_PATH;
    set +o nounset
    MY_PATH=$(append_path "${MY_PATH}" "${MY_ARG}")
    set -o nounset
    assertEquals "append_path" "/new/pathelement" "${MY_PATH}"
}

test_prepend_path() {
    MY_PATH="/opt/dir1";
    MY_PATH=$(prepend_path "/opt/dir2" "${MY_PATH}")
    assertEquals "prepend_path" "/opt/dir2:/opt/dir1" "${MY_PATH}"
}

test_prepend_path_ignoreDuplicate() {
    MY_PATH="/opt/dir1:/opt/dir2:/opt/dir3";
    
    assertEquals "prepend_path, ignore dupe in front" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prepend_path "/opt/dir1" "${MY_PATH}");
    
    assertEquals "prepend_path, ignore dupe in middle" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prepend_path "/opt/dir2" "${MY_PATH}");
    
    assertEquals "prepend_path, ignore dupe at end" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prepend_path "/opt/dir3" "${MY_PATH}");
        
    # sanity check
    assertEquals "sanity check, not a dupe" "/opt/dir4:/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prepend_path "/opt/dir4" "${MY_PATH}" ); 
}

test_prepend_path_toEmpty() {
    MY_ARG="/new/pathelement";
    unset MY_PATH;
    set +o nounset
    MY_PATH=$(prepend_path "${MY_ARG}" "${MY_PATH}")
    set -o nounset
    assertEquals "prepend_path" "/new/pathelement" "${MY_PATH}"
}

############################################################
# Function: array_length, get the number of elements in an array
# Usage:
#   array_length array_name
# Example 'indirect reference' to an array. Pass in the name
# of the array rather than the value.
# E.g. 
#   declare -a my_arr=( val1 val2 val3);
#   echo "$( array_length my_arr )";
#   # for debugging
#   declare -p my_arr 
#   echo "my_arr.length: ${#my_arr[@]}";
############################################################
array_length() {
  if [[ $# -eq 0 ]]; then
    echo "0";
    return
  fi

  local array_name="${1}";
  local array_ref="${array_name}[@]"
  if [[ -z ${!array_name+x} ]]; then
    echo "0";
    return
  fi
  declare -a arr_copy=( "${!array_ref}" )
  echo "${#arr_copy[@]}";
}

test_array_length() {
    unset my_arr;
    assertEquals "array_length, <unset>" "0" "$(array_length my_arr)";
    
    my_arr=();
    assertEquals "array_length, <empty array>" "0" "$(array_length my_arr)";
    
    my_arr=("");
    assertEquals "array_length, <empty string>" "1" "$(array_length my_arr)";
    
    my_arr=(" ");
    assertEquals "array_length, <white space>" "1" "$(array_length my_arr)";
    
    my_arr=( "apples" );
    assertEquals "array_length, (1 item)" "1" "$(array_length my_arr)";
    
    my_arr=( "apples" "tomatoes" );
    assertEquals "array_length, (2 items)" "2" "$(array_length my_arr)";
            
    my_arr=( apples tomatoes tomahtoes );
    assertEquals "array_length, (3 items)" "3" "$(array_length my_arr)";
    
    my_arr=( "one element" );
    assertEquals "array_length, with spaces" "1" "$(array_length my_arr)";
    
    my_arr=( "APPLE" "BANANA" "ORANGE" "" "NOT" "WITH SPACE" );
    assertEquals "array_length, (6 items)" "6" "$(array_length my_arr)";
}

test_is_set() {
  local my_var;
  unset my_var;
  is_set "my_var" \
    && fail "is_set 'my_var' (not set): expected false"
  
  my_var="";
  is_set "my_var" \
     || fail "is_set 'my_var' (empty): expected true"
  
  my_var="non empty string"
  is_set "my_var" \
    || fail "is_set 'my_var' (non empty): expected true"

  unset my_var;
  ! is_set "my_var" \
     || fail "! is_set (unset): expecting true"
}

# not_empty, false when unset
test_not_empty() {
  local my_var;
  unset my_var;
  not_empty "my_var" \
    && fail "not_empty (unset): expecting false"
  my_var="";
  not_empty "my_var" \
    && fail "not_empty (empty): expecting false"
  my_var="not empty"
  not_empty "my_var" \
    || fail "not_empty (not empty): expecting true"
}

test_is_empty() {
  local my_var;
  unset my_var;
  is_empty "my_var" \
      && fail "is_empty (not set): expected false"
  local my_var=;
  is_empty "my_var" \
     || fail "is_empty (my_var=): expected true"
  my_var='';
  is_empty "my_var" \
     || fail "is_empty (my_var=''): expected true"
  my_var="non empty value"
  is_empty "my_var" \
     && fail "is_empty (non empty value): expected false"
}

test_not_set() {
  not_set "GP_TEST_VAR" || fail "Expecting false"
  if not_set "GP_TEST_VAR"; then
    echo "GP_TEST_VAR is not set"
  fi
}

test_is_true() {
  # 0 is true
  local my_var=0;
  if ! is_true "my_var"; then
      fail "is_true 'my_var', 0 is true";
  fi
  # 'true' is true
  my_var="true";
  if ! is_true "my_var"; then
      fail "is_true 'my_var', 'true' is true";
  fi
  # 'TRUE' is true
  my_var="TRUE";
  if ! is_true "my_var"; then
      fail "is_true 'my_var', 'TRUE' is true";
  fi
  # non-zero is false
  my_var=1;
  if is_true "my_var"; then
    fail "is_true 'my_var', my_var=1: expected 'false', non-zero is false";
  fi
  # 'false' is not true
  my_var="false";
  if is_true "my_var"; then
      fail "is_true 'my_var', 'false' is not true";
  fi
  # 'FALSE' is not true
  my_var="FALSE";
  if is_true "my_var"; then
      fail "is_true 'my_var', 'FALSE' is not true";
  fi
  # (empty string) is not true
  my_var="";
  if is_true "my_var"; then
      fail "is_true 'my_var', (empty string) is not true";
  fi
  # any other string is not true
  my_var="truthy";
  if is_true "my_var"; then
      fail "is_true 'my_var', any other string is not true";
  fi
  # no arg
  if is_true; then
      fail "is_true (no arg) is not true"
  fi
  # unset
  unset my_var;
  if is_true "my_var"; then
      fail "is_true 'my_var', not set is not true";
  fi
}

test_is_valid_var() {
  if is_valid_var; then
      fail "is_valid_var (no arg): expected 'false'"
  fi
  if is_valid_var ""; then
      fail "is_valid_var (empty arg): expected 'false'"
  fi
  if is_valid_var "space in name"; then
      fail "is_valid_var 'space in name': expected 'false'"
  fi
  if is_valid_var "9numberAtStart"; then
      fail "is_valid_var '9numberAtStart': expected 'false'"
  fi
  if is_valid_var "arg;semi"; then
      fail "is_valid_var 'arg;semi': expected 'false'"
  fi
  if is_valid_var "arg; \`ls /\`"; then
      fail "is_valid_var 'arg;semi': expected 'false'"
  fi
  if ! is_valid_var "good"; then
      fail "is_valid_var 'good': expected 'true'"
  fi
  if ! is_valid_var "__under_score"; then
      fail "is_valid_var '__under_score': expected 'true'"
  fi
  if ! is_valid_var "number_at_end_01"; then
      fail "is_valid_var 'number_at_end_01': expected 'true'"
  fi
}

test_is_valid_r_version() {
  is_valid_r_version \
    && fail "is_valid_r_version (no arg): expected 'false'"
  is_valid_r_version "2.15.3" \
    || fail "is_valid_r_version '2.15.3': expected 'true'"
  is_valid_r_version "2.15" \
    || fail "is_valid_r_version '2.15': expected 'true'"
  is_valid_r_version "2" \
    || fail "is_valid_r_version '2': expected 'true'"
}

############################################################
# Function: arr_create
#   Create a new array, by indirect reference
# Usage:
#   arr_create array-name
############################################################
arr_create() {
  if [[ "$#" -eq 0 ]] || ! is_valid_var "$1"; then
    # debug: echo "Invalid bash variable" 1>&2 ; 
    return 1 ;
  fi
  local -r array_name="$1"
  # The following line can be replaced with 'declare -ag $array_name=\(\)'
  # Note: For some reason when using 'declare -ag $array_name' without the parentheses will make 'declare -p' fail
  eval $array_name=\(\)
}

############################################################
# Function: arr_push
#   Add item to end of array, by indirect reference
# Usage:
#   arr_push array-name value
############################################################
arr_push() { 
  if [[ "$#" -eq 0 ]] || ! is_valid_var "$1"; then
    # debug: echo "Invalid bash variable" 1>&2 ; 
    return 1 ;
  fi
  local -r array_name="$1"
  shift
  if [[ "$#" -eq 0 ]]; then
    # debug: echo "Missing arg"
    return 1 ;
  fi
  local -r value="$1"

  declare -p "${array_name}" > /dev/null 2>&1
  if [[ $? -eq 1 ]]; then
    # debug: echo "Bash variable [${1}] doesn't exist" 1>&2 ; 
    return 1 ;
  fi
  eval $array_name[\$\(\(\${#${array_name}[@]}\)\)]=\$value
}

############################################################
# Function: arr_get
#   Get the nth value by indirect reference to the named array
#   $array-name[$array-index]
# Usage:
#   arr_get array-name array-index
############################################################
arr_get() {
  if [[ "$#" -eq 0 ]]; then
    # debug: echo "arr_get: Missing arg, 'array-name' <= '\$1'" 1>&2 ;
    echo ""
    return
  fi
  if ! is_valid_var "$1"; then
    # debug: echo "arr_get: Invalid bash variable, 'array-name' <= '\$1'" 1>&2 ; 
    echo ""
    return
  fi
  local -r array_name="${1}";
  shift
  if [[ "$#" -eq 0 ]]; then
    # debug: echo "arr_get: Missing arg, 'array-index' <= '\$2'" 1>&2 ;
    echo ""
    return
  fi
  local -i idx="${1}"
  local array_ref="${array_name}[@]"
  if [[ -z ${!array_name+x} ]]; then
    # debug: echo "arr_get: Array reference not defined: '!$array_name'" 1>&2 ;
    echo ""
    return
  fi

  declare -a arr_copy=( "${!array_ref}" )
  local -i length="${#arr_copy[@]}"
  if [[ $idx -lt 0 ]] || [[ $idx -ge $length ]]; then
    # debug: echo "index out of range: ${array_name}[${idx}]" 1>&2 ;
    echo ""
    return
  fi
  echo "${arr_copy[$idx]}";
}

############################################################
# Function: arr_get_unchecked
#   Get the nth value by indirect reference to the named array
#   $array-name[$array-index]
# Usage:
#   arr_get array-name array-index
# Note: this version does not check array bounds
############################################################
arr_get_unchecked() {
  if [[ "$#" -eq 0 ]] || ! is_valid_var "$1"; then
    # debug: echo "Missing arg or invalid bash variable" 1>&2 ; 
    return 1 ;
  fi
  local -r array_name="$1"
  shift
  if [[ "$#" -eq 0 ]]; then
    # debug: echo "Missing arg"
    return 1 ;
  fi
  local -i array_index="$1"
  local array_ref="${array_name}[$array_index]"
  echo "${!array_ref}"
}

test_arr_push() {
  unset my_arr
  my_arr_name="my_arr"
  arr_create $my_arr_name
  arr_push $my_arr_name "APPLE"
  arr_push $my_arr_name "BANANA"
  arr_push $my_arr_name "ORANGE"
  arr_push $my_arr_name ""
  arr_push $my_arr_name "NOT"
  arr_push $my_arr_name "WITH SPACE"

  # debug: declare -p $my_arr_name;

  # direct reference, requires actual variable name, 'my_arr'
  assertEquals "array_length, direct ref" "6" "$(array_length my_arr)";
  assertEquals "$my_arr_name[0]" "APPLE" "${my_arr[0]}"
  assertEquals "$my_arr_name[1]" "BANANA" "${my_arr[1]}"

  # indirect reference, '!my_arr_name'
  assertEquals "array_length, indirect ref" "6" "$(array_length $my_arr_name)";
  #assertEquals "$my_arr_name[0]" "APPLE" "${!my_arr_name[0]}"
  local array_ref="${my_arr_name}[1]"
  assertEquals "$my_arr_name[1]" "BANANA" "${!array_ref}"

  assertEquals "$my_arr_name[0]" "APPLE" "$(arr_get $my_arr_name 0)"
  assertEquals "$my_arr_name[1]" "BANANA" "$(arr_get $my_arr_name 1)"
  assertEquals "$my_arr_name[2]" "ORANGE" "$(arr_get $my_arr_name 2)"
  assertEquals "$my_arr_name[3]" "" "$(arr_get $my_arr_name 3)"
  assertEquals "$my_arr_name[4]" "NOT" "$(arr_get $my_arr_name 4)"
  assertEquals "$my_arr_name[5]" "WITH SPACE" "$(arr_get $my_arr_name 5)"
  
  assertEquals "arr_get, missing array-name" "" "$(arr_get)"
  assertEquals "arr_get, undefined array-name" "" "$(arr_get bogus_arr_name 0)"
  assertEquals "arr_get, invalid array-name" "" "$(arr_get 'invalid array name' 0)"
  assertEquals "arr_get, missing index" "" "$(arr_get $my_arr_name)"
  assertEquals "arr_get, index out of bounds [6]" "" "$(arr_get $my_arr_name 6)"
  assertEquals "arr_get, index out of bounds [-1]" "" "$(arr_get $my_arr_name '-1')"
}

# for debugging initialization of dir, file, and base variables
# no tests run
function echoDirname() { 
    echo "GP_SCRIPT_DIR=${GP_SCRIPT_DIR}";
    echo "__gp_script_file=${__gp_script_file}";
    echo "__gp_script_base=${__gp_script_base}";
    echo "__test_script_dir=${__test_script_dir}";
}

source ${SHUNIT2_HOME}/src/shunit2
