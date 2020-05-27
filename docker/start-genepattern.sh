#!/bin/bash
VERSION="NULL"
HELP=0
name='genepattern'

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -v|--version) VERSION="$2"; shift ;;
        -h|--help) HELP=1 ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

if [[ $HELP > 0  ]]
then
    echo "Start a GenePattern server in a docker container"
    echo "Arguments:"
    echo "-v|--version <version_tag> Version of GenePattern container to retrieve when starting the first time"
    exit
fi

if [[  $VERSION == "NULL"  ]]
then
    echo "No GenePattern version provided with the -v flag.  Using default of v3.9.11-rc.5-b253"
    VERSION="v3.9.11-rc.5-b253"
fi


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
    echo "Using GenePattern server container version: $VERSION"
    echo "Pulling docker image...  genepattern/genepattern-server:$VERSION"
    # docker pull genepattern/genepattern-server:$VERSION
    echo "Setting up local directories, this will take up to 30 seconds  genepattern/genepattern-server:$VERSION"
    if [[ -d resources ]]
    then
        var=`date +"%FORMAT_STRING"`
        now=`date +"%m_%d_%Y"`
        now=`date +"%Y-%m-%d"`
        echo "${now}"
        mv resources resources_${now}
    fi
    echo "test run of genepattern/genepattern-server:$VERSION"
    docker run --name tmpserver -d -t genepattern/genepattern-server:$VERSION sleep 30s 
    # give the container a moment to start
    sleep 5s
    docker cp tmpserver:/opt/genepattern/resources  ./resources
    docker stop tmpserver
    docker rm tmpserver

     
    # update the bind mount for docker
    sed -i.bu s+__PPWWDD__+$PWD+ ./resources/config_custom.yaml 
    
    # create the other directories we want external to the container
    mkdir -p jobResults
    mkdir -p users
    mkdir -p taskLib
    echo "Starting new GenePattern server container with name $name"
    docker run -v $PWD:$PWD -v /var/run/docker.sock:/var/run/docker.sock -w $PWD -v $PWD/resources:/opt/genepattern/resources  -v $PWD/taskLib:/opt/genepattern/taskLib -v $PWD/jobResults:/opt/genepattern/jobResults -v $PWD/users:/opt/genepattern/users  -p 8888:8888 -p 8080:8080  -d --name "$name" genepattern/genepattern-server:$VERSION /opt/genepattern/StartGenePatternServer
fi

