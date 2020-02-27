#!/bin/bash

#
# We want a copy of the resources dir with all its contents appropriate for this release of GP so
# we take it from the one installed within the container.  When we run the server later we will mount
# this copy back to the same location
#
echo "Briefly starts the docker container to copy its resources directory external to the container."
docker run --name tmpserver -t liefeld/gpserver sleep 30s
docker cp tmpserver:/opt/genepattern/resources ./resources2
docker stop tmpserver
docker rm tmpserver

# create the other directories we want external to the container
mkdir -r jobResults
mkdir -p users
mkdir -p taskLib

echo "Now you must edi the resources/config_custom.yaml file to set the value of "
echo "   \"job.docker.bind_src\" to be this directory which will be where you run"
echo " the container".



