#!/bin/bash
export SSH_AUTH_SOCK=~/.ssh/ssh_auth_sock

# we grab the job number and set the path to the slightly different path that we would
 find 
# on the expanse server.  This is obviously not super portable
JOB_NUMBER=${PWD##*/}   
JOB_DIR="/expanse/projects/mesirovlab/genepattern/servers/ucsd.prod/jobResults/${JOB_N
UMBER}"

ssh login.expanse.sdsc.edu "cd ${JOB_DIR};$@"
