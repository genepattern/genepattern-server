#!/bin/bash

#
# Script for POSTing data to the UploadReceiver servlet
# Launch this from LSF to simulate load on the server.
# For example, the following command:
#     bsub -P gp_loadtest -o %J.out -e %J.err ./uploadTest.sh test test test_01.bin 300
#     bsub -P gp_loadtest -o %J.out -e %J.err ./uploadTest.sh test test test_01.bin 300
# will upload a 3 GB file (300 parts of a copy of the test.bin file) to the server as user test with password test.

# The server url is hard-coded to gpdev.broadinstitute.org.

# Because there can only be one file with the same name for each user account, you need to assign a new new for each time that you run the script.
# E.g. two submit two distint files do the following:
#
#     bsub -P gp_loadtest -o %J.out -e %J.err ./uploadTest.sh test test test_02.bin 300
#     bsub -P gp_loadtest -o %J.out -e %J.err ./uploadTest.sh test test test_03.bin 300

gpuser=$1
gppass=$2
filename=$3
partitionCount=$4

echo gpuser: ${gpuser}
echo gppass: ${gppass}
echo filename: ${filename}
echo partitionCount: ${partitionCount}

cp test.bin ${filename}

curl -v --dump-header cookie_${gpuser}_${filename}.txt -u "${gpuser}:${gppass}" -H user-agent:IGV -d javax.faces.ViewState=javax.faces.ViewState http://gpdev.broadinstitute.org/gp/login.jsf

x=0
while [ $x -lt $partitionCount ]
do
  curl -v --cookie cookie_${gpuser}_${filename}.txt -F file=@./${filename} -F partitionCount=${partitionCount} -F partitionIndex=$x http://gpdev.broadinstitute.org/gp/UploadReceiver
  x=$(( $x + 1 ))
done
