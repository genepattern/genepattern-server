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
    echo "No GenePattern version provided with the -v flag.  Using default of v3.9.11-rc.5-b250.3"
    VERSION="v3.9.11-rc.5-b252"
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
    echo "Pulling docker image..."
    docker pull genepattern/genepattern-server:$VERSION
    echo "Setting up local directories, this will take up to 30 seconds"
    if [[ -f resources ]]
    then
        var=`date +"%FORMAT_STRING"`
        now=`date +"%m_%d_%Y"`
        now=`date +"%Y-%m-%d"`
        echo "${now}"
        cp -r resources resources_${now}
    fi

    docker run --name tmpserver -d -t genepattern/genepattern-server:v3.9.11-rc.5-b250.3 sleep 30s 
    # give the container a moment to start
    sleep 5s
    docker cp tmpserver:/opt/genepattern/resources  ./resources
    docker stop tmpserver
    docker rm tmpserver

    echo "XXX A"
    exit 
    # update the bind mount for docker
    sed s+__PPWWDD__+$PWD+ ./resources/config_custom.yaml 
    # and backup for old one with the developers bath
    sed s+"/Users/liefeld/GenePattern/gp_dev/genepattern-server/docker"+$PWD+ ./resources/config_custom.yaml 

    echo "XXX B"
    # create the other directories we want external to the container
    mkdir -p jobResults
    mkdir -p users
    mkdir -p taskLib
    echo "XXX c"
    echo "Starting new GenePattern server container with name $name"
    docker run -v $PWD:$PWD -v /var/run/docker.sock:/var/run/docker.sock -w $PWD -v $PWD/resources:/opt/genepattern/resources  -v $PWD/taskLib:/opt/genepattern/taskLib -v $PWD/jobResults:/opt/genepattern/jobResults -v $PWD/users:/opt/genepattern/users  -p 8888:8888 -p 8080:8080  -d --name "$name" genepattern/genepattern-server:$VERSION /opt/genepattern/StartGenePatternServer
    echo "XXX d"
fi

