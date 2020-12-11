#!/bin/bash

# $1 is the file path and name
# $2 is the bucket name
# $3 is the aws profile to use

# add an AWS profile statement if needed
if [ "$3" ]; then
   PROFILE=" --profile $3 "
   echo "Profile is $PROFILE"
else  
   PROFILE=""
fi

aws s3 presign s3://${2}/$1 $PROFILE


