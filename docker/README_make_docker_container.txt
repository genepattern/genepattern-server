To make the docker container for a build,

1. Download the installer version (linux no-VM) into this folder as GPserver.bin (the default name)

2. execute on a shell "docker build -t genepattern/genepattern-server:<version> ." where you put in the version tag.

3. To run it, 
   a. First run the setupgp.sh script. This will copy the resources dir outside of the container so that when you run it, the config files and DB (HSQL) are external to the container so that you can upgrade the server later.

   b. Next use the 'startgp.sh' script in this directory.  This creates a container named "GenePattern-Server"  the first time but thereafter only starts the previously created one (or does nothing if its already running).  

4. If you want to run a newer version of the container, first "docker stop GenePattern-Server" then "docker rm GenePattern-Server" to get rid of the old container.  Then you pull the latest image and run startgp.sh again in the same directory as before to pickup the old configuration, jobResults and filles. If a newer version has configuration updates or changes in resources, you will need to manually re-run setupgp.sh but first backup the resources directory so that you can manually merge the 2.


TBD - hook this into GenePattern server build process somehow
