#!/bin/bash 

#
# e.g. 
# runTwill.sh http://node255.broad.mit.edu:7070 ./gp_scripts/tutorial.twill 50
#

# twill-sh
twill_sh=./twill-0.9b1/twill-sh 

url=$1
script=$2
num=$3

i=0
while [  $i -lt $num ]; do
   let i=i+1 
   echo iteration $i of $num
   ${twill_sh} -u ${url} ${script}
done