#!/bin/bash

.  ~/.bash_profile

S3_ROOT=s3://moduleiotest

aws s3 sync  $S3_ROOT$1 $1 --profile genepattern
