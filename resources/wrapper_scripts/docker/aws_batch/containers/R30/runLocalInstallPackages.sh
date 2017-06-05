#!/bin/bash

##################################################
# MODIFICATION FOR R PACKAGE INSTALLATION
##################################################
if [ -f "$TASKLIB/r.package.info" ]
then
        echo "$TASKLIB/r.package.info found."
        Rscript /build/source/installPackages.R $TASKLIB/r.package.info
else
        echo "$TASKLIB/r.package.info not found."
fi


# run the module
echo $@
$@ 
echo "{ \"exit_code\": $? }"


