To make the docker container for a build,

1. Download the installer version (linux no-VM) into this folder as GPserver.bin (the default name)

2. execute on a shell "docker build -t genepattern/genepattern-server:<version> ." where you put in the version tag.

3. To run it, use the 'startgp.sh' script in this directory.  Note that it presumes you want to keep a container and re-use it so it creates it the first time but thereafter only starts the previously created one.  If you want to run a newer version of the container, first "docker rm GenePattern-Server" to get rid of the old container.  Also look inside the script for instructions on externalizing configuration, jobResults and user uploads.

TBD - hook this into GenePattern server build process somehow
