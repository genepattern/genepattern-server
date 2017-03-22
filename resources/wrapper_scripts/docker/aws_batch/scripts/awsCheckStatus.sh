#!/bin/bash

.  ~/.bash_profile

aws batch describe-jobs --jobs $1 --profile genepattern | python -c "import sys, json; print( json.load(sys.stdin)['jobs'][0]['status'])"



