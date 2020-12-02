#!/bin/bash

# create a temp input file for the lamda because of problems properly escaping the quotes
IN_FILE="${RANDOM}.json"
echo "{ \"input\": { \"name\":\"$1\", \"filetype\":\"$2\", \"bucket\":\"$3\"}}" >$IN_FILE

# add an AWS profile statement if needed
if [ "$5" ]; then
   PROFILE=" --profile $5 "
   echo "Profile is $PROFILE"
else  
   PROFILE=""
fi

/Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  $4 --profile genepattern

#echo /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  $4 --profile genepattern

# remove the temp input file to avoid wasting space
rm $IN_FILE
