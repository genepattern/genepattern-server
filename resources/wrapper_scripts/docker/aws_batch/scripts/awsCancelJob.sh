#!/bin/bash

.  ~/.bash_profile

aws batch terminate-job --job-id $1 --reason "Cancelled from Geneattern" --profile genepattern 



