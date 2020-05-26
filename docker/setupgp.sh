#!/bin/bash

#
# We want a copy of the resources dir with all its contents appropriate for this release of GP so
# we take it from the one installed within the container.  When we run the server later we will mount
# this copy back to the same location
#
echo "Briefly starts the docker container to copy its resources directory external to the container."
docker run --name tmpserver -d -t genepattern/genepattern-server:v3.9.11-rc.5-b250.3 sleep 30s &
# give it a moment to start
sleep 5s
docker cp tmpserver:/opt/genepattern/resources  ./resources

docker stop tmpserver
docker rm tmpserver

# create the other directories we want external to the container
mkdir -p jobResults
mkdir -p users
mkdir -p taskLib

echo "Now you must edit the resources/config_custom.yaml file to set the value of "
echo "   \"job.docker.bind_src\" to be this directory which will be where you run"
echo " the container".



