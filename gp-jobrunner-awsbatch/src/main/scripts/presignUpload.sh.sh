#!/bin/bash

echo "POSITIONAL $@"

# create a temp input file for the lamda because of problems properly escaping the quotes
IN_FILE="${RANDOM}.json"
echo "{ \"input\": { \"name\":\"$2\", \"filetype\":\"$4\", \"bucket\":\"$6\", \"numParts\":\"$8\" }}" >$IN_FILE
echo "\n inputs are \n\n { \"input\": { \"name\":\"$2\", \"filetype\":\"$4\", \"bucket\":\"$6\", \"numParts\":\"$8\" }}\n\n"


# add an AWS profile statement if needed
if [ "$12" ]; then
   PROFILE=" --profile ${12} "
   echo "Profile is $PROFILE"
else  
   PROFILE=""
fi

echo "CALLING \n\n /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  ${10} --profile genepattern  \n\n"

/Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  ${10} $PROFILE

#echo /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  $5 --profile genepattern

# remove the temp input file to avoid wasting space
#rm $IN_FILE
