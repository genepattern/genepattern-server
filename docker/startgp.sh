#!/bin/bash

name='GenePattern-Server'
#
# Uncomment the 2 lines below (and comment the similar lines further down the script) to have the container keep 
# the files and configuration external to the container.
# This assumes you want them in $PWD/users, $PWD/jobResults and $PWD/resources on your host computer
# 
#[[ $(docker ps -f "name=$name" --format '{{.Names}}') == $name ]] ||
#docker run -v $PWD:$PWD -w $PWD -p 8888:8888 -p 8080:8080 -v $PWD/resources:/opt/genepattern/resources -v $PWD/jobResults:/opt/genepattern/jobResults -v $PWD/users:/opt/genepattern/users liefeld/gpserver /opt/genepattern/StartGenePatternServer
#
#

#
# The version below keeps all files (uploads, job results, configuration) internal to the container.  If you "docker remove <container_id>"
# you will lose all customizations, uploads and job results.  It is however convenient for testing or temporary servers. 
#
[[ $(docker ps -f "name=$name" --format '{{.Names}}') == $name ]] ||
docker run -v $PWD:$PWD -w $PWD -p 8888:8888 -p 8080:8080  -d --name "$name" liefeld/gpserver /opt/genepattern/StartGenePatternServer

