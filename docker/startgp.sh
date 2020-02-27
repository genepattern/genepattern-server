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
# you will lose all customizations, uploads and job results.  It is however convenient for testing or temporary servers. 
#
if  [[ $(docker ps -f "name=$name" --format '{{.Names}}') == $name ]] 
then
    echo "Container $name already running" 
elif  [[ $(docker ps -af "name=$name" --format '{{.Names}}') == $name ]]
then
    echo "Restarting stopped container $name"
    docker start $name
else 
    echo "Starting new GenePattern server container with name $name"
    docker run -v $PWD:$PWD -v /var/run:/var/run -w $PWD -v $PWD/resources:/opt/genepattern/resources  -v $PWD/taskLib:/opt/genepattern/taskLib -v $PWD/jobResults:/opt/genepattern/jobResults -v $PWD/users:/opt/genepattern/users  -p 8888:8888 -p 8080:8080  -d --name "$name" liefeld/gpserver /opt/genepattern/StartGenePatternServer

fi

