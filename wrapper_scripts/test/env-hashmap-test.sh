#!/bin/bash

setUp() {
    source ../env-lookup.sh
}

tearDown() {
    clearValues;
}

#
# basic stress-testing of the env-hashmap.sh script
#
testEnvHashMap_duplicate() {
    source ../env-hashmap.sh
    putValue "A" "a"
    putValue "B" "b"
    putValue "C" "c"
    
    actual=$(indexOf 'A')
    #echo "myKeys=${myKeys[@]}"
    #echo "myVals=${myVals[@]}"
    assertEquals "indexOf('A')" "0" "$actual"
    assertEquals "indexOf('B')" "1" $(indexOf 'B')
    assertEquals "indexOf('C')" "2" $(indexOf 'C')
    assertEquals "myVals[2]" "c" ${myVals[2]}
    idx=$(indexOf 'C');
    assertEquals 'myVals[$idx]' "c" ${myVals[$idx]}
    assertEquals "getValue('C')" "c" $(getValue 'C')
    
    source ../env-lookup.sh
    assertEquals "getValue('C')" "c" $(getValue 'C')
}

testEmptyEnvMap() {
    local key="Java-1.7";
    assertEquals "indexOf($key)" "-1" $(indexOf $key)
    assertEquals "getValue($key)" "$key" $(getValue $key)
}

testInitEnvMap() {
  startSkipping
#  source ../env-lookup.sh
#  key="Java_1_7"
#  envMap['Java_1_7']='java/1.7.0_51'
#  envMap['GCC_4_9']='GCC-4.9'
  
#  echo "envMap=${envMap[@]}"
    
#  array_exp 'envMap["Java_1_7"]="java/1.7.0_51"'
#  array_exp 'envMap["GCC_4_9"]="GCC-4.9"'
#  echo "envMap=${envMap[@]}"
  
  actual=array_exp 'envMap["Java_1_7"]'
  assertEquals "modOrReplace Java_1_7" "java/1.7.0_51" "${actual}"
    
#  actual=$(modOrReplace "Java_1_7")
#  assertEquals "modOrReplace Java_1_7" "java/1.7.0_51" "${actual}"
    
}

testEnvInit_with_array() {
  startSkipping
  source ../env-lookup.sh
  source ../env-lookup-broad-centos5.sh
  arg="Java_1_7"
  actual=$(modOrReplace "${arg}")
  assertEquals "test" "${arg}" "${actual}"
}

# . shunit2
. ${SHUNIT2_HOME}/src/shunit2