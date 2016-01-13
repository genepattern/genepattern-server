#!/usr/bin/env bash

#
# shunit2 test cases for wrapper script code
#

# called before each test
setUp() {
    unset GP_ENV_CUSTOM
    unset GP_DEBUG    
    source ../env-hashmap.sh
    test_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
}

# called after each test
tearDown() {
    clearValues;
    unset GP_ENV_CUSTOM;
    unset GP_DEBUG
}

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
# Test cases
# ----------
# 1) when the key is not in the map, return the key
# 2) when the key is one of the canonical keys and there is no customization, return the default value
# 3) when the key is one of the canonical keys and there is a customization, return the custom value
# 4) special-case: map one key to multiple values, e.g. R-2.15 requires GCC for a particular (custom) installation
#    Something like: IFS=', ' read -a array <<< "$string", oldIFS="$IFS", ..., IFS="$oldIFS"


#
# Get the root name from the given moduleName.
# 
# Usage: extractRootName <moduleName>
#
# Example,
#    echo extractRootName "R/2.15.3"
#    > "R"
#
function extractRootName() {
    rootName="${1%%\/*}";
    echo "$rootName";
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
# basic file exists test
#
testFileExists() {
    assertTrue "fileExists('env-test.sh')" "[ -e 'env-test.sh' ]"
    
    prefix="env-";
    suffix="test.sh";
    assertTrue "fileExists('$prefix$suffix')" "[ -e $prefix$suffix ]"
}

#
# basic stress-testing of the env-hashmap.sh script
#

testEnvHashMap() {
    putValue "A" "a"
    putValue "B" "b"
    putValue "C" "c"
    
    assertTrue "hasIndex('A')"  "[ $(indexOf 'A') -gt -1 ]"
    assertTrue "hasIndex('B')"  "[ $(indexOf 'B') -gt -1 ]"
    assertTrue "hasIndex('C')"  "[ $(indexOf 'C') -gt -1 ]"
    assertTrue "not hasIndex('D')"  "! [ $(indexOf 'D') -gt -1 ]"
    assertEquals "getValue('A')" "a" $(getValue 'A')
    assertEquals "getValue('B')" "b" $(getValue 'B')
    assertEquals "getValue('C')" "c" $(getValue 'C')

    # sourcing env-hashmap.sh a second time should not clear the hash map 
    source ../env-hashmap.sh
    assertEquals "after source env-hasmap.sh a 2nd time, getValue('C')" "c" $(getValue 'C')
}

testPutValue_NoKey() {
    putValue 'Java-1.7'
    assertEquals "getValue('Java-1.7')" "Java-1.7" "$(getValue 'Java-1.7')"
}

testPutValueWithSpaces() {
    putValue "A" "a" 
    putValue "B" "a space" 
    assertEquals "indexOf('B')" "1" $(indexOf 'B')
    assertEquals "getValue('B')" "a space" "$(getValue 'B')"
}

testPutValueWithDelims() {
    putValue "A" "a" 
    putValue "B" "val1, val2" 
    assertEquals "indexOf('B')" "1" $(indexOf 'B')
    assertEquals "getValue('B')" "val1, val2" "$(getValue 'B')"
}

testPutKeyWithSpaces() {
    putValue "A" "a"
    putValue "B KEY" "b"
    assertEquals "indexOf('B KEY')" "1" $(indexOf 'B KEY')
    assertEquals "getValue('B KEY')" "b" "$(getValue 'B KEY')"
}

testGetValueWithDelims() {
    putValue "R-2.15" "R-2.15, GCC-4.9" 
    assertEquals "indexOf('R-2.15')" "0" $(indexOf 'R-2.15')
    assertEquals "getValue('R-2.15')" "R-2.15, GCC-4.9" "$(getValue 'R-2.15')"

    # split into values
    value="$(getValue 'R-2.15')"
    IFS=', ' read -a valueArray <<< "$value"
    assertEquals "valueArray[0]" "R-2.15" "${valueArray[0]}"
    assertEquals "valueArray[1]" "GCC-4.9" "${valueArray[1]}"
    
    # example code, step through each value
    #for index in "${!valueArray[@]}"
    #do
    #  echo "$index ${valueArray[index]}"
    #done
}

#
# basic testing of env-lookup.sh utility functions
#
testInitPath() {
    source ../env-lookup.sh
    assertEquals "no arg" "$(scriptDir)/" $(initPath)
    assertEquals "relative path" "$(scriptDir)/env-custom.sh" $(initPath 'env-custom.sh')
    assertEquals "full path" "/opt/genepattern" $(initPath '/opt/genepattern')
}

testInitCustomValuePath() {
    source ../env-lookup.sh
    assertEquals "bash test" "$(scriptDir)/env-custom.sh" "$(initPath 'env-custom.sh')"
    assertEquals "default" "$(scriptDir)/env-custom.sh" "$(initCustomValuePath)"
    assertEquals "as arg, relative" "$(scriptDir)/env-lookup-shunit2.sh" "$(initCustomValuePath 'env-lookup-shunit2.sh')"
    assertEquals "as arg, fq path" "/opt/env-lookup-shunit2.sh" "$(initCustomValuePath '/opt/env-lookup-shunit2.sh')"
    export GP_ENV_CUSTOM="env-custom-broad-centos5.sh"
    assertEquals "env, relative" "$(scriptDir)/env-custom-broad-centos5.sh" "$(initCustomValuePath)"
    unset GP_ENV_CUSTOM;
    export GP_ENV_CUSTOM="/opt/env-custom-broad-centos5.sh"
    assertEquals "env, fq path" "/opt/env-custom-broad-centos5.sh" "$(initCustomValuePath)"
}

# 1) when the key is not in the map, return the key
testGetValue_NoEntry() {
    local key="Java-1.7";
    assertEquals "indexOf($key)" "-1" $(indexOf $key)
    assertEquals "getValue($key)" "$key" $(getValue $key)
}

# 2) when the key is one of the canonical keys and there is no customization, return the default value
testGetValue_CanonicalEntry() {
    source ../env-lookup.sh
    assertTrue "indexOf('Java-1.7') before sourceEnvDefault" "[ "-1" -eq "$(indexOf 'Java-1.7')" ]"
    initValues
    assertTrue "indexOf('Java-1.7')" "[ "-1" -ne "$(indexOf 'Java-1.7')" ]"
}

# 3) when the key is one of the canonical keys and there is a customization, return the custom value
# 4) special-case: map one key to multiple values, 
#    e.g. R-2.15 requires GCC for a particular (custom) installation
#    Something like: IFS=', ' read -a array <<< "$string", oldIFS="$IFS", ..., IFS="$oldIFS"
testGetValue_CustomEntry() {
    source ../env-lookup.sh
    sourceEnvDefault
    sourceEnvCustom "${test_script_dir}/env-lookup-shunit2.sh"
    
    assertEquals "custom value" ".matlab_2010b_mcr" "$(getValue Matlab-2010b-MCR)"
    assertEquals "custom values" "R-3.1, GCC-4.9" "$(getValue R-3.1)"
}

testInitCustomValuesFromEnv() {
    export GP_ENV_CUSTOM="${test_script_dir}/env-lookup-shunit2.sh"
    source ../env-lookup.sh
    initValues

    assertEquals "canonical value" "Java-1.7" "$(getValue 'Java-1.7')"
    assertEquals "unset value" "my-dotkit" "$(getValue 'my-dotkit')"
    assertEquals "custom value" ".matlab_2010b_mcr" "$(getValue Matlab-2010b-MCR)"
    assertEquals "custom values" "R-3.1, GCC-4.9" "$(getValue R-3.1)"
}

testExportEnvForLibMesa() {
    source ../env-lookup.sh
    sourceEnvDefault
    sourceEnvCustom "${test_script_dir}/env-lookup-shunit2.sh"

    # this is a special-case for the Broad hosted servers
    assertEquals "before" "" "$(echo $RGL_USE_NULL)"
    initEnv '.libmesa_from_matlab-2014b'
    assertEquals "after" "TRUE" "$(echo $RGL_USE_NULL)"    
}

testAddEnv() {
    source ../env-lookup.sh
    sourceEnvDefault
    sourceEnvCustom "${test_script_dir}/env-lookup-shunit2.sh"
    
    addEnv 'Java-1.7';
    addEnv 'R-3.1';
    
    # expecting three entries
    assertEquals "_runtime_environments.size" "3" "${#_runtime_environments[@]}"
}

testAddEnvIU() {
    source ../env-lookup.sh
    sourceEnvCustom "env-custom-IU.sh"
    assertEquals "check values" 'gcc/4.7.2, R/3.0.1' "$(getValue 'R-3.0')"

    addEnv 'R-3.0'
    assertEquals "_runtime_envs[0]" "gcc/4.7.2" "${_runtime_environments[0]}"
    assertEquals "_runtime_envs[1]" "R/3.0.1" "${_runtime_environments[1]}"
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
    assertEquals "$input, args[1]" "" "${args[1]}"

    input="MY_ARG="
    IFS='=' read -r -a args <<< "$input"
    assertTrue "$input, hasEquals" "[[ $input == *=* ]]"
    assertEquals "$input, args.length", "1" "${#args[@]}"
    assertEquals "$input, args[0]" "MY_ARG" "${args[0]}"
    assertEquals "$input, args[1]" "" "${args[1]}"
    
    input="=MY_VAL"
    IFS='=' read -r -a args <<< "$input"
    assertTrue "$input, hasEquals" "[[ $input == *=* ]]"
    assertEquals "$input, args.length", "2" "${#args[@]}"
    assertEquals "$input, args[0]" "" "${args[0]}"
    assertEquals "$input, args[1]" "MY_VAL" "${args[1]}" 
}

testExportEnv_basic() {
    source ../env-lookup.sh
    unset MY_KEY
    
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
    source ../env-lookup.sh
    unset MY_KEY

    local input="=MY_VAL"
    assertEquals "exportEnv $input, ignored, not previously set" "" "$MY_KEY"
    assertTrue   "exportEnv $input, MY_KEY is not set" "[ -z ${MY_KEY+x} ]"
    
    export MY_KEY=MY_VAL   # <---- reset the value
    assertEquals "exportEnv $input, ignored, previously set" "MY_VAL" "$MY_KEY"
    assertTrue   "exportEnv $input, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
}

# exportEnv 'MY_KEY=' should unset the value
testExportEnv_unset() {
    source ../env-lookup.sh
    unset MY_KEY

    local input="MY_KEY="
    exportEnv "$input"
    assertEquals "exportEnv $input, no previous value" "" "$MY_KEY"
    assertTrue   "exportEnv $input, MY_KEY is not set" "[ -z ${MY_KEY+x} ]"
    
    export MY_KEY=MY_VALUE
    assertEquals "sanity check, export previous value" "MY_VALUE" "$MY_KEY"
    assertTrue   "sanity check, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
    
    exportEnv "$input"
    assertEquals "exportEnv $input, previous value" "" "$MY_KEY"
    assertTrue   "exportEnv $input, previous value, MY_KEY is not set" "[ -z ${MY_KEY+x} ]"
}

# exportEnv 'MY_KEY' should set an empty value
testExportEnv_setToEmpty() {
    source ../env-lookup.sh
    unset MY_KEY

    local input="MY_KEY"    
    exportEnv "$input"
    assertEquals "exportEnv $input, no previous value" "" "$MY_KEY"
    assertTrue   "exportEnv '$input, MY_KEY is set" "! [ -z ${MY_KEY+x} ]"
    
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
    
    unset GP_DEBUG
    assertEquals "default initEnv, no debug output" "Hello, World!" "$(../run-with-env.sh -u Java echo 'Hello, World!')"    
}

testRunWithEnv_custom_env_arg() {
    export GP_DEBUG="true";
    local env_custom="${test_script_dir}/env-lookup-shunit2.sh";
    
    local expected=$'loading R-3.1 ...\nloading GCC-4.9 ...\nHello, World!';
    assertEquals "set GP_ENV_CUSTOM with '-c' arg" "$expected" "$(../run-with-env.sh -c ${env_custom} -u R-3.1 echo 'Hello, World!')"
}

#
# test setenv as cmdline arg, e.g 
# ../run-with-env.sh ... -e <env.key=env.value> ... 
#
testRunWithEnv_setenv_on_cmdLine() {
     assertEquals "no args" "MY_FLAG=" "$( './print-my-flag.sh' )" 
     
     local cmd="${test_script_dir}/print-my-flag.sh"
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
    assertEquals "run java" "$expected" "$('../run-java.sh' '-version')"
}

testRunJava_custom_env_arg() {
    export GP_DEBUG="true";
    local expected=$'loading custom/java ...'
    assertEquals "run java" "$expected" "$('../run-java.sh' '-c' './test/env-lookup-shunit2.sh' '-version')"
}

testRunRJava() {
    export GP_DEBUG="true";
    local expected=$'loading R-2.5 ...\nloading Java-1.7 ...'
    assertEquals "run R2.5" "$expected" "$('../run-rjava.sh' '2.5' '-version')"
}

testRunRJava_custom_env_arg() {
    export GP_DEBUG="true";
    local env_custom="${test_script_dir}/env-lookup-shunit2.sh";
    local expected=$'loading custom/R/2.5 ...\nloading custom/java ...'
    assertEquals "run R2.5" "$expected" "$('../run-rjava.sh' '-c' './test/env-lookup-shunit2.sh' '2.5' '-version')"
}

. ${SHUNIT2_HOME}/src/shunit2
