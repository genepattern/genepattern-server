#!/bin/bash

name='GenePattern-Server'

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
    docker run -v $PWD:$PWD -v /var/run/docker.sock:/var/run/docker.sock -w $PWD -v $PWD/resources:/opt/genepattern/resources  -v $PWD/taskLib:/opt/genepattern/taskLib -v $PWD/jobResults:/opt/genepattern/jobResults -v $PWD/users:/opt/genepattern/users  -p 8888:8888 -p 8080:8080  -d --name "$name" liefeld/gpserver /opt/genepattern/StartGenePatternServer

fi

