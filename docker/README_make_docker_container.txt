INSTALLER_URL=https://github.com/genepattern/genepattern-server/releases/download/v3.9.11-rc.5-b250/GPserver.bin
docker build --build-arg GP_INSTALLER=${INSTALLER_URL} -t genepattern/genepattern-server:test  docker

To make the docker container for a build,

1. Decide what version of GenePattern you want and get the URL to the installer.  This container build is parameterized to wget the installer from a URL as part of the build process.  For convenience you can set the installer URL as an environment variable

e.g.   INSTALLER_URL=https://github.com/genepattern/genepattern-server/releases/download/v3.9.11-rc.5-b250/GPserver.bin

2. execute the build passing in the INSTALLER_URL as a build argument.  Adjust the tag to the desired value.

e.g.  docker build --build-arg GP_INSTALLER=${INSTALLER_URL} -t genepattern/genepattern-server:test  .


3. To run it, 
   a. First run the setupgp.sh script. This will copy the resources dir outside of the container so that when you run it, the config files and DB (HSQL) are external to the container so that you can upgrade the server later.

   b. Next use the 'startgp.sh' script in this directory.  This creates a container named "GenePattern-Server"  the first time but thereafter only starts the previously created one (or does nothing if its already running).  

4. If you want to run a newer version of the container, first "docker stop GenePattern-Server" then "docker rm GenePattern-Server" to get rid of the old container.  Then you pull the latest image and run startgp.sh again in the same directory as before to pickup the old configuration, jobResults and filles. If a newer version has configuration updates or changes in resources, you will need to manually re-run setupgp.sh but first backup the resources directory so that you can manually merge the 2.


** IMPORTANT SECURITY CONSIDERATIONS **

This setup allows the GenePatterrn docker container to call docker on the host to launch modules.  It will mount the file system defined in config_custom.yaml at JOB_DOCKER_BIND_SRC into those containers and hypothetically a malicious container could access anything under that point in the file system as root.  Therefore there is some risk involved here.  

If you prefer, you can run it with all containers run inside the GenePattern container.  The main change to the server run would be to NOT mount the docker.sock into the container


