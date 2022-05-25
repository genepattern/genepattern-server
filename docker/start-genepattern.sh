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

docker_cmd="docker"
command -v "$docker_cmd" >/dev/null || {
    echo "\
Command not found: '${docker_cmd}'

Make sure to install Docker and configure your GenePattern Server.

To install Docker  ...
  See: https://docs.docker.com/get-docker/

" >&2; exit 127;
}


if [[  $VERSION == "NULL"  ]]
then
    echo "No GenePattern version provided with the -v flag.  Using default of v3.9_22.02_b377"
    VERSION="v3.9_21.04.05_b325"
    VERSION="v3.9_22.02_b377"
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
        echo "Saving old resources dir"
        now=`date +"%Y-%m-%d-%H:%M"`
        echo "${now}"
        mv  ./resources ./resources_${now}
        echo "     -- copied to resources_${now}"
    fi
    echo "test run of genepattern/genepattern-server:$VERSION on ${now}"
    docker run --name tmpserver -d -t genepattern/genepattern-server:$VERSION sleep 30s 
    # give the container a moment to start
    sleep 5s
   
    if [ -d "resources_${now}" ]; then
        docker cp tmpserver:/opt/genepattern/resources  "./resources"
        echo "     The following existing files will be preserved from the old resources "
        echo "     into the new structure to maintain continuity."
        echo "         copy: resources/config_custom.yaml (if present)"
        echo "         copy: resources/genepattern.properties"
        echo "         copy: resources/GenePatternDB.properties	"
        echo "         copy: resources/GenePatternDB.script"
        echo "         copy: resources/userGroups.xml"
        echo "         copy: resources/permissionsMap.xml"	  
        echo "         copy: resources/database_custom.properties"
  
        cp ./resources_${now}/config_custom.yaml ./resources/
        cp ./resources_${now}/genepattern.properties ./resources/
        cp ./resources_${now}/GenePatternDB.properties ./resources/
        cp ./resources_${now}/GenePatternDB.script ./resources/
        cp ./resources_${now}/userGroups.xml ./resources/

        if [ -f ./resources_${now}/database_custom.properties ]; then
            cp resources_${now}/database_custom.properties ./resources/
        fi
        if [ -f resources_${now}/GenePatternDB.log ]; then
        	cp resources_${now}/GenePatternDB.log ./resources/
        fi 
    else
        echo "Did not find resources_${now}"
        docker cp tmpserver:/opt/genepattern/resources  ./resources
        docker cp tmpserver:/opt/genepattern/StartGenePatternServer.lax ./resources/StartGenePatternServer.lax 
    fi
    docker stop tmpserver
    docker rm tmpserver

     
    # update the bind mount for docker
    sed -i.bu s+__PPWWDD__+$PWD+ ./resources/config_custom.yaml 
    
    # create the other directories we want external to the container
    mkdir -p jobResults
    mkdir -p users
    mkdir -p taskLib

    # bypass registration
    echo "registeredServer=true" >> resources/StartGenePatternServer.lax

    echo "Starting new GenePattern server container with name $name"
    docker run -v $PWD:$PWD -v /var/run/docker.sock:/var/run/docker.sock -w $PWD  --mount type=bind,source=$PWD/resources/StartGenePatternServer.lax,target=/opt/genepattern/StartGenePatternServer.lax   -v $PWD/resources:/opt/genepattern/resources  -v $PWD/taskLib:/opt/genepattern/taskLib -v $PWD/jobResults:/opt/genepattern/jobResults -v $PWD/users:/opt/genepattern/users  -p 8888:8888 -p 8080:8080  -d --name "$name" genepattern/genepattern-server:$VERSION /opt/genepattern/StartGenePatternServer
fi

