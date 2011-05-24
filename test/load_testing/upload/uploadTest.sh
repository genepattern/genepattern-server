#!/bin/bash
curl -v --dump-header cookie.txt -u "test:test" -H user-agent:IGV -d javax.faces.ViewState=javax.faces.ViewState http://gpdev.broadinstitute.org/gp/login.jsf

x=0
while [ $x -lt 2000 ]
do
  curl -v --cookie cookie.txt -F file=@./test.txt -F partitionCount=2000 -F partitionIndex=$x http://gpdev.broadinstitute.org/gp/UploadReceiver
  x=$(( $x + 1 ))
done