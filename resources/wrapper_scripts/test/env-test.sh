#!/usr/bin/env bash

#
# shunit2 test cases for wrapper script code
#

# called once before running any test
oneTimeSetUp()  {
    readonly __test_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    readonly __parent_dir="$(cd "${__test_script_dir}/.."  && pwd)"
    readonly __gp_common_script="${__parent_dir}/gp-common.sh";
    
    # just for illustration, not presently used
    readonly __gp_script_file="${GP_SCRIPT_DIR}/$(basename "${BASH_SOURCE[0]}")";
    readonly __gp_script_base="$(basename ${__gp_script_file} .sh)";

    # Exit on error. Append || true if you expect an error.
    set -o errexit
    # Exit on error inside any functions or subshells.
    set -o errtrace
    # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
    set -o nounset
    # Catch the error in case mysqldump fails (but gzip succeeds) in `mysqldump |gzip`
    set -o pipefail
    # Turn on traces, useful while debugging but commented out by default
    #set -o xtrace

    # source 'gp-common.sh'
    if [ -f "${__gp_common_script}" ]; then
        source "${__gp_common_script}";
    else
        fail "gp-common-script not found: ${__gp_common_script}";
    fi
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
# All tests are run after 'source gp-common.sh'
#
# items to test ... 
#
# After parseArgs ...
#     * get the site defaults script
#     * ... confirm that it was loaded
#     * get the optional site customization script, confirm that it was loaded
#     * ... confirm that it was loaded
#     * get the list of declared environment variables
#     * ... confirm that they were set
#     * get the list of declared environment modules
#
# After loadEnvironmentModules ...
#     * ... confirm that the environment modules were loaded
#     * special-case: set an environment variable in the site-customization script
#     * special-case: override environment module 
#
#     * confirm that the environment modules were loaded
#
#
# Glossary
#     'environment variable' - an environment variable is set via the export command,
#         export NAME=VALUE
#
#     'environment module' - an environment module is a named requirement that is loaded in 
#         a platform specific way.
#         Also known as a 'dotkit' or a 'package' or a 'software requirement' or a 'library'. 
#         It is a dendency or requirement that must be present (aka loaded into the environment,
#         or otherwise provisioned) before running the module command line.
#

# test GP_SCRIPT_DIR 
test_gp_script_dir() {
  assertEquals "GP_SCRIPT_DIR" "${__parent_dir}" \
    "${GP_SCRIPT_DIR:-}";

  # the remaining tests are for debugging,
  #     they may not be essential for production 
  assertEquals "gp::script_dir" "${__parent_dir}" \
    "$(gp::script_dir)";
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
}

testParseArgs_c_arg_not_set() {
    parseArgs '-c' '-u' 'Java-1.8' 'java' '--version';

    assertEquals "'-c' arg" "" "${__gp_env_custom_arg}"
    assertEquals "env_custom" "${GP_SCRIPT_DIR}/env-custom.sh" \
        "${__gp_env_custom_script}"
    assertEquals "__gp_module_cmd" \
        "java --version" \
        "${__gp_module_cmd[*]:0}";
}

testParseArgs_c_arg_at_end() {
    parseArgs "-c";

    assertEquals "'-c' arg" "" "${__gp_env_custom_arg}"
    assertEquals "env_custom" "${GP_SCRIPT_DIR}/env-custom.sh" \
        "${__gp_env_custom_script}"
    assertEquals "__gp_module_cmd" \
        "" \
        "${__gp_module_cmd[*]:0}"
}

testParseArgs_default_full_test() {
    local -a args=('-u' 'Java-1.8' 'java' '--version');
    parseArgs "${args[@]}";

    # debugEnv
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

testSiteDefaultsScript() {
    parseArgs
}

testParseArgs_default() {
    local -a args=("-u" "Java-1.8" "java");
    parseArgs "${args[@]}";
    #assertEquals "__gp_env_custom_arg" "" "${__gp_env_custom_arg}"
    #assertEquals "__gp_env_custom_script" "" "${__gp_env_custom_script}"
}

#
# for debugging initialization of dir, file, and base variables
# no tests run
function echoDirname() { 
    echo "GP_SCRIPT_DIR=${GP_SCRIPT_DIR}";
    echo "__gp_script_file=${__gp_script_file}";
    echo "__gp_script_base=${__gp_script_base}";
    echo "__test_script_dir=${__test_script_dir}";
}

#
# basic extract prefix test
# to extract 'R' from the pattern 'R/2.15.3'
testRootModuleName() {
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

#
# basic '-z' condition test
#
#     [[ -z "${_my_var+x}" ]] is true when _my_var is not set
#
# Hint: using short-circuit instead of if-then statements
# asserTrue ...
#     <condition> || fail <message>
# assertFalse 
#     <condition> && fail <message>
#
testVarSet() {
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

#
# basic file exists test
#
testFileExists() {
    assertTrue "fileExists('env-test.sh')" "[ -e 'env-test.sh' ]"
    
    prefix="env-";
    suffix="test.sh";
    assertTrue "fileExists('$prefix$suffix')" "[ -e $prefix$suffix ]"
}

testAppendPath() {
    MY_PATH="/opt/dir1";
    MY_PATH=$(appendPath "${MY_PATH}" "/opt/dir2")
    assertEquals "appendPath" "/opt/dir1:/opt/dir2" "${MY_PATH}"
}

testAppendPath_ignoreDuplicate() {
    MY_PATH="/opt/dir1:/opt/dir2:/opt/dir3";
    
    assertEquals "appendPath, ignore dupe in front" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(appendPath "${MY_PATH}" "/opt/dir1");
    
    assertEquals "appendPath, ignore dupe in middle" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(appendPath "${MY_PATH}" "/opt/dir2");
    
    assertEquals "appendPath, ignore dupe at end" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(appendPath "${MY_PATH}" "/opt/dir3");
        
    # sanity check
    assertEquals "sanity check, not a dupe" "/opt/dir1:/opt/dir2:/opt/dir3:/opt/dir4" \
        $(appendPath "${MY_PATH}" "/opt/dir4"); 
}

testAppendPath_toEmpty() {
    MY_ARG="/new/pathelement";
    unset MY_PATH;
    set +o nounset
    MY_PATH=$(appendPath "${MY_PATH}" "${MY_ARG}")
    set -o nounset
    assertEquals "appendPath" "/new/pathelement" "${MY_PATH}"
}

testPrependPath() {
    MY_PATH="/opt/dir1";
    MY_PATH=$(prependPath "/opt/dir2" "${MY_PATH}")
    assertEquals "prependPath" "/opt/dir2:/opt/dir1" "${MY_PATH}"
}

testPrependPath_ignoreDuplicate() {
    MY_PATH="/opt/dir1:/opt/dir2:/opt/dir3";
    
    assertEquals "prependPath, ignore dupe in front" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prependPath "/opt/dir1" "${MY_PATH}");
    
    assertEquals "prependPath, ignore dupe in middle" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prependPath "/opt/dir2" "${MY_PATH}");
    
    assertEquals "prependPath, ignore dupe at end" "/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prependPath "/opt/dir3" "${MY_PATH}");
        
    # sanity check
    assertEquals "sanity check, not a dupe" "/opt/dir4:/opt/dir1:/opt/dir2:/opt/dir3" \
        $(prependPath "/opt/dir4" "${MY_PATH}" ); 
}

testPrependPath_toEmpty() {
    MY_ARG="/new/pathelement";
    unset MY_PATH;
    set +o nounset
    MY_PATH=$(prependPath "${MY_ARG}" "${MY_PATH}")
    set -o nounset
    assertEquals "prependPath" "/new/pathelement" "${MY_PATH}"
}

#
# basic stress-testing of the env-hashmap.sh script
#
#

#function __count_args() { echo "${#@}"; }
function __count_args() { echo $#; }

_my_size_of() {
    #local my_var="${1}[@]";
    #echo "$(__size_of $(echo "${!my_var}"))";
    local array_name="${1}";
    #echo "array_name: ${array_name}";
    local my_var="${array_name}[@]";
    #echo "my_var: ${my_var}";
    #__size_of $(echo "${!my_var:0}");
    #echo "$(__size_of $(echo "${!my_var:0}"))";
    #__count_args "${!my_var:0}";
    __size_of "${!my_var:0}";
}

#
# get the number of elements in an array
#
# this makes an 'indirect reference' to the 
# named array
#
# Usage:
#     declare -a my_arr=();
#     echo "$( numValues my_arr )";
#
#function numValues() { 
#    array="${1}[@]";
#    echo $(__size_of "${!array:0}");
#}

#numValues() {
#    local array_name="${1}";
#    local my_var="${array_name}[@]";
#    echo "$(__size_of $(echo "${!my_var}"))";
#}

numValues() {
    #local array_name="${1}";
    local my_var="${1}[@]";
    #__size_of "${!my_var:0}";
    
    echo $(__size_of "${!my_var:0}");
}

debugNumValues() {
    echo "__size_of keys: $(__size_of "${_hashmap_keys[@]:0}")";
    echo "__size_of envs: $(__size_of "${__gp_module_envs[@]:0}")";
    
    local my_var="_hashmap_keys[@]";
    echo "__my_var: ${my_var}";
    echo "!__my_var: ${!my_var:0}";
    #echo "#!__my_var: ${#!my_var:0}";
    numValues _hashmap_keys
    
    local -a my_copy=("${!my_var:0}"); 
    echo "#my_copy: ${#my_copy[@]}";
    #arrayClone=("${oldArray[@]}")
}


testNumValues() {
    assertEquals "__count_args" "0" "$(__count_args)"
    assertEquals "__count_args" "4" "$(__count_args this is a test)"
    declare -a my_arr=("this" "is" "a" "test");
    assertEquals "__count_args my_arr" "4" "$(__count_args "${my_arr[@]}")"
    my_arr=("this is" "a" "test");
    assertEquals "__count_args with spaces" "3" "$(__count_args "${my_arr[@]}")"
    
    unset my_arr;
    assertEquals "numValues <unset>" "0" "$(_my_size_of my_arr)";
    
    my_arr=();
    assertEquals "numValues <empty>" "0" "$(_my_size_of my_arr)";
    
    my_arr=( 1 );
    assertEquals "numValues (1 item)" "1" "$(_my_size_of my_arr)";

    
    my_arr=( "one element" );
    assertEquals "numValues with spaces" "1" "$(_my_size_of my_arr)";
        
    #declare -a my_arr=(1 2 3);
    #echo "numValues my_arr: $(numValues my_arr)";    
}

testEnvHashmapInit() {
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

testEnvHashMap() {
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

testPutValue_NoKey() {
    putValue 'Java-1.7'
    assertEquals "getValue('Java-1.7')" "Java-1.7" "$(getValue 'Java-1.7')"
}

testPutValueWithSpaces() {
    assertEquals "numKeys initial" "0" $(numKeys)
    assertTrue "numKeys -eq 0, initial" "[ 0 -eq $(numKeys) ]"
    assertTrue "isEmpty (initial)" "[ isEmpty ]"
    
    putValue "A" "a" 
    putValue "B" "a space" 
    assertEquals "__indexOf('B')" "1" $(__indexOf 'B')
    assertEquals "getValue('B')" "a space" "$(getValue 'B')"
}

testPutValueWithDelims() {
    putValue "A" "a" 
    putValue "B" "val1, val2" 
    assertEquals "__indexOf('B')" "1" $(__indexOf 'B')
    assertEquals "getValue('B')" "val1, val2" "$(getValue 'B')"
}

testPutKeyWithSpaces() {
    putValue "A" "a"
    putValue "B KEY" "b"
    assertEquals "__indexOf('B KEY')" "1" $(__indexOf 'B KEY')
    assertEquals "getValue('B KEY')" "b" "$(getValue 'B KEY')"
}

testGetValueWithDelims() {
    putValue "R-2.15" "R-2.15, GCC-4.9" 
    assertEquals "__indexOf('R-2.15')" "0" $(__indexOf 'R-2.15')
    assertEquals "getValue('R-2.15')" "R-2.15, GCC-4.9" "$(getValue 'R-2.15')"

    # split into values
    value="$(getValue 'R-2.15')"
    IFS=', ' read -a valueArray <<< "$value"
    assertEquals "valueArray[0]" "R-2.15" "${valueArray[0]}"
    assertEquals "valueArray[1]" "GCC-4.9" "${valueArray[1]}"
}

#
# basic testing of convertPath() utility function
#
testConvertPath() {
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



# test setEnvCustomScript() utility function
testEnvCustomScript() {
    function envCustomScript() {
        setEnvCustomScript "${1-}";
        echo "${__gp_env_custom_script}";
    }

    local readonly _fq_path="/opt/gp/my custom with spaces.sh";
    unset GP_ENV_CUSTOM;
    assertEquals "default" \
        "${__parent_dir}/env-custom.sh" \
        "$(envCustomScript)"
    assertEquals "empty string" \
        "${__parent_dir}/env-custom.sh" \
        "$(envCustomScript '')"
        assertEquals "custom -c arg, relative" \
        "${__parent_dir}/env-custom-macos.sh" \
        "$(envCustomScript 'env-custom-macos.sh')"
    assertEquals "custom -c arg, fully qualified" \
        "${_fq_path}" \
        "$(envCustomScript "${_fq_path}")"

    GP_ENV_CUSTOM="env-custom-macos.sh";
    assertEquals "export GP_ENV_CUSTOM, relative" \
        "${__parent_dir}/env-custom-macos.sh" \
        "$(envCustomScript)";
    GP_ENV_CUSTOM="${_fq_path}";
    assertEquals "export GP_ENV_CUSTOM, relative" \
        "${_fq_path}" \
        "$(envCustomScript)";
        unset GP_ENV_CUSTOM;
}

# test parseArgs(), with '-c' <env-custom> arg
testParseArgs_env_custom_arg() {
  args=('-c' 'env-custom-macos.sh' '-u' 'Java-1.8' 'java');
  parseArgs "${args[@]}";
  assertEquals "__gp_env_custom_arg" "env-custom-macos.sh" "${__gp_env_custom_arg}"
  assertEquals "__gp_env_custom_script" "${GP_SCRIPT_DIR}/env-custom-macos.sh" "${__gp_env_custom_script}"
}

# test parseArgs, with '-e' GP_ENV_CUSTOM=<env-custom> arg
testParseArgs_environment_arg() {
  args=('-e' 'GP_ENV_CUSTOM=env-custom-macos.sh' '-u' 'Java-1.8' 'java');
  parseArgs "${args[@]}";
  assertEquals "__gp_env_custom_arg" "" "${__gp_env_custom_arg}"
  assertEquals "__gp_env_custom_script" "${GP_SCRIPT_DIR}/env-custom-macos.sh" "${__gp_env_custom_script}"
}

# test parseArgs, with export GP_ENV_CUSTOM=<env-custom>
testParseArgs_environment_var() {
  export GP_ENV_CUSTOM="env-custom-macos.sh";
  args=('-u' 'Java-1.8' 'java');
  parseArgs "${args[@]}";
  assertEquals "__gp_env_custom_arg" "" "${__gp_env_custom_arg}"
  assertEquals "__gp_env_custom_script" "${GP_SCRIPT_DIR}/env-custom-macos.sh" "${__gp_env_custom_script}"
}

testInitCustomValuesFromEnv() {
    source "../env-default.sh";
    source "${__test_script_dir}/env-lookup-shunit2.sh";

    assertEquals "canonical value" "Java-1.7" "$(getValue 'Java-1.7')"
    assertEquals "unset value" "my-dotkit" "$(getValue 'my-dotkit')"
    assertEquals "custom value" ".matlab_2010b_mcr" "$(getValue Matlab-2010b-MCR)"
    # special-case: map one key to multiple values
    assertEquals "custom values" "R-3.1, GCC-4.9" "$(getValue R-3.1)"
}

# 1) when the key is not in the map, return the key
testGetValue_NoEntry() {
    local key="Java-1.7";
    assertEquals "__indexOf($key)" "-1" $(__indexOf $key)
    assertEquals "getValue($key)" "$key" $(getValue $key)
}

# 2) when the key is one of the canonical keys and there is no customization, return the default value
testGetValue_CanonicalEntry() {
    assertTrue "__indexOf('Java-1.7') before sourceEnvDefault" "[ "-1" -eq "$(__indexOf 'Java-1.7')" ]"
    source ../env-default.sh
    assertTrue "__indexOf('Java-1.7')" "[ "-1" -ne "$(__indexOf 'Java-1.7')" ]"
}

# 3) when the key is one of the canonical keys and there is a customization, return the custom value
# 4) special-case: map one key to multiple values, 
#    e.g. R-2.15 requires GCC for a particular (custom) installation
#    Something like: IFS=', ' read -a array <<< "$string", oldIFS="$IFS", ..., IFS="$oldIFS"
testGetValue_CustomEntry() {
    source "../env-default.sh";
    source "${__test_script_dir}/env-lookup-shunit2.sh"

    assertEquals "custom value" ".matlab_2010b_mcr" "$(getValue Matlab-2010b-MCR)"
    assertEquals "custom values" "R-3.1, GCC-4.9" "$(getValue R-3.1)"
}

testExportEnvForLibMesa() {
    source "../env-default.sh";
    source "${__test_script_dir}/env-lookup-shunit2.sh"

    # this is a special-case for the Broad hosted servers
    assertTrue   "RGL_USE_NULL should not be set" "[ -z ${RGL_USE_NULL+x} ]"
    
    assertEquals "before" "" "$(echo ${RGL_USE_NULL:-})"
    initEnv '.libmesa_from_matlab-2014b'
    assertEquals "after" "TRUE" "$(echo $RGL_USE_NULL)"    
}

testAddEnv() {
    source "../env-default.sh";
    source "${__test_script_dir}/env-lookup-shunit2.sh"

    addEnv 'Java-1.7';
    addEnv 'R-3.1';
    
    # expecting three entries
    assertEquals "__gp_module_envs.size" "3" "${#__gp_module_envs[@]}"
}

#
# Example site customization, alias for canonical environment name
#
testAddEnv_alias_mcr() {
    source "env-custom-for-testing.sh"
    assertEquals "alias 'Matlab-2013a-MCR' <- 'matlab/2013a'" 'matlab/2013a' "$(getValue 'Matlab-2013a-MCR')"
    
    addEnv 'Matlab-2013a-MCR'
    assertEquals "addEnv 'Matlab-2013a-MCR', _runtime_envs[0]" "matlab/2013a" "${__gp_module_envs[0]}"
}


#
# Example site customization for R-3.0
#      add 'gcc' dependency, R-3.0 depends on gcc
#
testAddEnv_dependency_r_3_0_on_gcc() {
    source "env-custom-for-testing.sh"
    assertEquals "check values" 'gcc/4.7.2, R/3.0.1' "$(getValue 'R-3.0')"

    addEnv 'R-3.0'
    assertEquals "_runtime_envs[0]" "gcc/4.7.2" "${__gp_module_envs[0]}"
    assertEquals "_runtime_envs[1]" "R/3.0.1" "${__gp_module_envs[1]}"
}

testAddEvn_set_default_java_version() {
    source "env-custom-for-testing.sh"
    addEnv 'Java'
    assertEquals "_runtime_envs[0]" "java/1.8.1" "${__gp_module_envs[0]}"
}

#
# low level, bash scripting specific test cases
#
join() {
    local IFS=$1;
    shift;
    echo "$*";
}

function testJoinArray() {
    declare -a myArray=('arg1' 'arg2');
    assertEquals "join, no delim" "arg1 arg2" "$(join ' ' ${myArray[@]})"
    assertEquals "join, custom delim" "arg1,arg2" "$(join ',' ${myArray[@]})"
}

#
# test parsing of "key=val" from a string
#
testBashParseArgs() {
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

testExportEnv_basic() {
    unset MY_KEY
    
    assertEquals "exportEnv (no arg), sanity check" "" "$(exportEnv)"
    exportEnv
    
    local input="MY_KEY=MY_VAL"
    exportEnv "$input"
    assertEquals "exportEnv $input" "MY_VAL" "$MY_KEY"
    assertTrue   "exportEnv $input, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
    
    exportEnv "MY_KEY=MY_VAL"   # <---- reset the value
    input="MY_KEY"
    exportEnv "$input"
    assertEquals "exportEnv $input, unset" "" "$MY_KEY"
    assertTrue   "exportEnv $input, expecting MY_KEY to be set" "! [ -z ${MY_KEY+x} ]"
    
}

# exportEnv '=MY_VAL', no left hand side value
testExportEnv_ignoreInvalidArg() {
    local input="=MY_VAL"
    exportEnv "$input"
}

# exportEnv 'MY_KEY=' should unset the value
testExportEnv_unset() {
    # setUp the test by defining MY_KEY 
    export MY_KEY=MY_VALUE
    assertEquals "sanity check, export previous value" "MY_VALUE" "$MY_KEY"
    assertTrue   "sanity check, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"

    # run the test, call exportEnv to unset MY_KEY
    local input="MY_KEY="
    exportEnv "$input"
    assertTrue   "exportEnv $input, MY_KEY is not set" "[ -z ${MY_KEY+x} ]"
}

# exportEnv 'MY_KEY' (no equals sign) should set an empty value
testExportEnv_setToEmpty() {
    local input="MY_KEY"

    # test 1: 'MY_KEY' not set
    unset MY_KEY
    assertTrue   "sanity check, MY_KEY is not set" "[ -z ${MY_KEY+x} ]"
    exportEnv "$input"
    assertEquals "exportEnv $input, no previous value" "" "$MY_KEY"
    assertTrue   "exportEnv '$input, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"

    # test 2: 'MY_KEY' is set
    export MY_KEY=MY_VALUE
    assertEquals "sanity check, export previous value" "MY_VALUE" "$MY_KEY"
    assertTrue   "sanity check, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
    exportEnv "$input"
    assertEquals "exportEnv $input, previous value" "" "$MY_KEY"
    assertTrue   "exportEnv $input, previous value, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
}

testRunWithEnv() {
    export GP_DEBUG="true"
    local expected=$'loading Java-1.7 ...\nHello, World!'
    TEST_OUT=$(../run-with-env.sh -u Java echo 'Hello, World!')
    assertEquals "default initEnv, with debug output" "$expected" "$TEST_OUT"
    
    unset GP_DEBUG
    assertEquals "default initEnv, no debug output" \
        "Hello, World!" \
        "$(../run-with-env.sh -u Java echo 'Hello, World!')"    
}

testRunWithEnv_custom_env_arg() {
    export GP_DEBUG="true";
    local env_custom="${__test_script_dir}/env-lookup-shunit2.sh";
    
    local expected=$'loading R-3.1 ...\nloading GCC-4.9 ...\nHello, World!';
    
    source "${__test_script_dir}/../env-default.sh";
    source "${env_custom}";
    #source env-lookup-shunit2.sh
    assertEquals "set GP_ENV_CUSTOM with '-c' arg" \
        "$expected" \
        "$(../run-with-env.sh -c ${env_custom} -u R-3.1 echo 'Hello, World!')"
}

#
# test setenv as cmdline arg, e.g 
# ../run-with-env.sh ... -e <env.key=env.value> ... 
#
testRunWithEnv_setenv_on_cmdLine() {
     assertEquals "no args" "MY_FLAG=" "$( './print-my-flag.sh' )" 
     
     local cmd="${__test_script_dir}/print-my-flag.sh"
     assertEquals "exportEnv MY_FLAG=MY_CMD_LINE_VALUE" \
         "MY_FLAG=MY_CMD_LINE_VALUE" \
         "$( '../run-with-env.sh' \
                 '-e' 'MY_UNUSED_FLAG'  \
                 '-e' '=BOGUS_VALUE' \
                 '-e' 'MY_UNSET_FLAG=' \
                 '-e' 'MY_EMPTY_FLAG' \
                 '-e' 'MY_FLAG=MY_CMD_LINE_VALUE' \
                 '-u' 'Java-1.7' \
                 $cmd)"
}

#
# test run-with-env.sh with an invalid command line option,
# make sure the script exits early rather than go into an infinite loop
#
testRunWithEnv_invalid_option() {
    unset GP_DEBUG
    TEST_OUT=$((../run-with-env.sh -Xmx512m -u Java echo 'Hello, World!') 2>&1)
    assertEquals "invalid '-Xmx512m' command line arg" "../run-with-env.sh: illegal option -- X" "$TEST_OUT"
}

testRunJava() {
    export GP_DEBUG="true";
    local expected=$'loading Java-1.7 ...'
    assertEquals "run java" "$expected" "$('../run-java.sh' '--' '-version')"
}

testRunJava_custom_env_arg() {
    export GP_DEBUG="true";
    local expected=$'loading custom/java ...'
    assertEquals "run java" "$expected" "$('../run-java.sh' '-c' './test/env-lookup-shunit2.sh' '--' '-version')"
}

#
# example mapping a single environment to multiple environments, e.g.
#     R-2.7=cairo,R-2.7
#
testMultiEnv() {
    export GP_DEBUG="true";
    local 
}

testRunRJava() {
    export GP_DEBUG="true";
    local expected=$'loading R-2.5 ...\nloading Java-1.7 ...'
    assertEquals "run R2.5" "$expected" "$('../run-rjava.sh' '2.5' '-version')"
}

testRunRJava_custom_env_arg() {
    export GP_DEBUG="true";
    local env_custom="${__test_script_dir}/env-lookup-shunit2.sh";
    local expected=$'loading custom/R/2.5 ...\nloading custom/java ...'
    assertEquals "run R2.5" "$expected" "$('../run-rjava.sh' '-c' './test/env-lookup-shunit2.sh' '2.5' '-version')"
}

#
# validate run-script.sh with no -a flag
#
testRunRscript_no_env_arch() {
    local script_dir=$( cd ../ && pwd )    
    local mock_patch_dir="/opt/genepattern/patches";
    export GP_DEBUG="false"
    
    local expected="${script_dir}/run-with-env.sh \
-c env-custom-macos.sh \
-u R-2.15 \
-e GP_DEBUG=FALSE \
-e R_LIBS= \
-e R_LIBS_USER=' ' \
-e R_LIBS_SITE=${mock_patch_dir}/Library/R/2.15 \
-e R_ENVIRON=${script_dir}/R/Renviron.gp.site \
-e R_ENVIRON_USER=${script_dir}/R/2.15/Renviron.gp.site \
-e R_PROFILE=${script_dir}/R/2.15/Rprofile.gp.site \
-e R_PROFILE_USER=${script_dir}/R/2.15/Rprofile.gp.custom \
Rscript \
--version"
    
    assertEquals "run R2.15" \
        "${expected}" \
        "$('../run-rscript.sh' \
            '-n' \
            '-c' 'env-custom-macos.sh' \
            '-v' '2.15' \
            '-p' ${mock_patch_dir} \
            '-l' '/opt/genepattern/tasks/MyModule.1' \
            '-m' 'FALSE' \
            '--' \
            '--version')"
}

#
# validate -a flag to run-rscript.sh command
#
testRunRscript_with_env_arch_arg() {
    local script_dir=$( cd ../ && pwd )    
    local mock_patch_dir="/opt/genepattern/patches";
    export GP_DEBUG="false"
    
    local expected="${script_dir}/run-with-env.sh \
-c env-custom-macos.sh \
-u R-2.15 \
-e GP_DEBUG=FALSE \
-e R_LIBS= \
-e R_LIBS_USER=' ' \
-e R_LIBS_SITE=${mock_patch_dir}/mock-env-arch/Library/R/2.15 \
-e R_ENVIRON=${script_dir}/R/Renviron.gp.site \
-e R_ENVIRON_USER=${script_dir}/R/2.15/Renviron.gp.site \
-e R_PROFILE=${script_dir}/R/2.15/Rprofile.gp.site \
-e R_PROFILE_USER=${script_dir}/R/2.15/Rprofile.gp.custom \
Rscript \
--version"
    
    assertEquals "run R2.15 with '-a' 'mock-env-arch' arg" \
        "${expected}" \
        "$('../run-rscript.sh' \
            '-n' \
            '-c' 'env-custom-macos.sh' \
            '-v' '2.15' \
            '-p' ${mock_patch_dir} \
            '-l' '/opt/genepattern/tasks/MyModule.1' \
            '-a' 'mock-env-arch' \
            '-m' 'FALSE' \
            '--' \
            '--version')"

    local expected_no_env_arch="${script_dir}/run-with-env.sh \
-c env-custom-macos.sh \
-u R-2.15 \
-e GP_DEBUG=FALSE \
-e R_LIBS= \
-e R_LIBS_USER=' ' \
-e R_LIBS_SITE=${mock_patch_dir}/Library/R/2.15 \
-e R_ENVIRON=${script_dir}/R/Renviron.gp.site \
-e R_ENVIRON_USER=${script_dir}/R/2.15/Renviron.gp.site \
-e R_PROFILE=${script_dir}/R/2.15/Rprofile.gp.site \
-e R_PROFILE_USER=${script_dir}/R/2.15/Rprofile.gp.custom \
Rscript \
--version"

#    # <wrapper-scripts>/run-rscript.sh -c <env-custom> -l <libdir> -p <patches> -a <env-arch>
#    assertEquals "run R2.15, env-arch not set" \
#        "${expected_no_env_arch}" \
#        "$('../run-rscript.sh' \
#            '-n' \
#            '-c' 'env-custom-macos.sh' \
#            '-a' \
#            '-v' '2.15' \
#            '-p' ${mock_patch_dir} \
#            '-l' '/opt/genepattern/tasks/MyModule.1' \
#            '-m' 'FALSE' \
#            '--' \
#            '--version')"

## <wrapper-scripts>/run-rscript.sh -c <env-custom> -l <libdir> -p <patches> -a <env-arch>
#    assertEquals "run R2.15 with '-e' 'GP_ENV_ARCH=mock-env-arch' arg" \
#        "${expected}" \
#        "$('../run-rscript.sh' \
#            '-n' \
#            '-c' 'env-custom-macos.sh' \
#            '-e' 'GP_ENV_ARCH=mock_env_arch' \
#            '-v' '2.15' \
#            '-p' ${mock_patch_dir} \
#            '-l' '/opt/genepattern/tasks/MyModule.1' \
#            '-m' 'FALSE' \
#            '--' \
#            '--version')"

}

#
# double-check the syntax of the initEnv function in the env-custom-macos.sh script
# These tests run on non-Mac systems; just validate that the bash syntax is correct
# and that the PATH is set to the expected R Library location
#
# Note: The existing library does not remove an item from the PATH; make sure that the
#     test cleans up after itself
#
testEnvCustomMacOs() {
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
}

#
# Prepend an element to the beginning of the path; 
# Usage: path=$(prependPath "${element}" "${path}")
#
function prependPath() {
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

#
# Append an element to the end of the path; 
# Usage: path=$(appendPath "${path}" "${element}")
#
function appendPath() {
    local path="${1}";
    local element="${2}";
    
    # Note, to check for a directory: [ -d "$element" ] 
    # To prepend, path="$element:$path"
    
    # if path is not set ... just set it to element
    # Note:  [ -z "${path+x}" ] checks if the 'path' variable is declared
    if [ -z "$path" ]; then
        #echo "2, path not set";
        path="$element";
    elif [[ ":$path:" != *":$element:"* ]]; then
        path="${path:+"$path:"}$element"
    fi
    # use echo to return a value
    echo "$path"
}




 source ${SHUNIT2_HOME}/src/shunit2
