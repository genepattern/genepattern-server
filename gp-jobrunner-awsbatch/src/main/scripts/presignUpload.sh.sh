#!/bin/bash

echo "started" > $DIR/started2.txt

echo "POSITIONAL $@"

# create a temp input file for the lamda because of problems properly escaping the quotes
IN_FILE="${RANDOM}.json"
echo              "{ \"input\": { \"name\":\"$2\", \"filetype\":\"$4\", \"bucket\":\"$6\", \"numParts\":\"$8\" }}" >$IN_FILE

echo "INFILE IS "
more $IN_FILE

echo "OUTFILE IS ${10}"

# add an AWS profile statement if needed
if [ "$12" ]; then
   PROFILE=" --profile ${12} "
   echo "Profile is $PROFILE"
else  
   PROFILE=""
fi
echo "Calling " >> $DIR/started2.txt

echo "CALLING  /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  ${10} --profile genepattern  "

/Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  ${10} $PROFILE

echo "finished call " >> $DIR/started2.txt

echo "Outfile contents are "
#more $OUT_FILE
#echo /Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload file://${IN_FILE}  $5 --profile genepattern

# remove the temp input file to avoid wasting space
#rm $IN_FILE

echo "finished " > $DIR/finished2.txt



