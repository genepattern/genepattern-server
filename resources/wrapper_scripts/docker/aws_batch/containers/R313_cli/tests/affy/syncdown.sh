#!/bin/bash
TEST_ROOT=/Users/liefeld/GenePattern/gp_dev/genepattern-server/resources/wrapper_scripts/docker/aws_batch/R313_cli/tests/affy
aws s3 sync s3://moduleiotest$TEST_ROOT/job_12345 ./job_12345 --profile genepattern



