#!/bin/bash

#
# shunit2 test cases for wrapper script code
#


# called before each test
setUp() {
    source ../env-hashmap.sh
    test_script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
}

# called after each test
tearDown() {
    clearValues;
    unset GP_ENV_CUSTOM;
}

# demo assertTrue
testAssertTrue() 
{
    assertTrue "Basic '-eq' test" "[ 1 -eq 1 ]"
}

# demo assertEquals
testAssertEquals() {
    assertEquals 'Basic test' 'Hello, World!' 'Hello, World!'
}

# Usage: arrayToString <array> [<delim>]
# for example:
#    declare -a argArray=('a' 'b');
#    arrayToString argArray[@]
function arrayToString() {
    declare -a argAry1=("${!1}")
    argAry1Str=$( IFS=$' '; echo "${argAry1[*]}" );
    echo "$argAry1Str";
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
    unset GP_ENV_CUSTOM;
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
    unset GP_ENV_CUSTOM;    
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
    assertTrue "indexOf('Java-1.7') before initCanonicalValues" "[ "-1" -eq "$(indexOf 'Java-1.7')" ]"
    initValues
    assertTrue "indexOf('Java-1.7')" "[ "-1" -ne "$(indexOf 'Java-1.7')" ]"
}

# 3) when the key is one of the canonical keys and there is a customization, return the custom value
# 4) special-case: map one key to multiple values, 
#    e.g. R-2.15 requires GCC for a particular (custom) installation
#    Something like: IFS=', ' read -a array <<< "$string", oldIFS="$IFS", ..., IFS="$oldIFS"
testGetValue_CustomEntry() {
    source ../env-lookup.sh

    initCanonicalValues
    initCustomValues "${test_script_dir}/env-lookup-shunit2.sh"
    
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

testAddEnv() {
    source ../env-lookup.sh
    initCanonicalValues
    initCustomValues "${test_script_dir}/env-lookup-shunit2.sh"
    
    addEnv 'Java-1.7';
    addEnv 'R-3.1';
    
    # expecting three entries
    assertEquals "runtimeEnvironments.size" "3" "${#runtimeEnvironments[@]}"
}

testParseCmdLine() {
    declare -a mockCmdLine=('run-with-env.sh' '-u' 'Java' '-u' 'R-2.15');
    source ../env-lookup.sh
    initCanonicalValues
    initCustomValues

}


. ${SHUNIT2_HOME}/src/shunit2
