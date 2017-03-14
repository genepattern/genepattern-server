
This dockerfile builds a R3.1.3 docker image and pre-installs many R packages into it.  At the moment the packages are installed by copying the r.package.info file into the image as well as the installPackages.r script and running the script as a part of the build process.  This builds an image that seems to work properly for GSEA version 14 and AffySTExpressionFileCreator.

Other additions to the image include
- Amazon AWS CLI (which requires python, pip etc)
- Java JRE  
- runS3OnBatch.sh - the script executed when the container is run in AWS batch, passed the source code and inputs via S3 urls

Usage:

This container includes R, some R packages, Java, Python and the Amazon Command Line interface.  It does not include any source code for any modules (except the r.package.info used to create it). There fore to use it to run a module you must 

1. mount a directory containing input files
2. mount a directory to be the working directory for the module
3. mount a directory with the source of the module (i.e. the GenePattern libdir for the module
4. Pass it a command line to execute that references the source and data via the paths you mounted

In the case of running the container locally the mount is a literal docker mount.  In the case of running on AWS Batch and using AWS S3 for the files, the runS3OnBatch.sh script in the container synchs what you put into S3 for source, inputs and workign directory with the file system inside the container. 

Usage like this is shown by the files tests/affy/runLocal.sh and tests/affy/runOnBatchAndS3.sh the first runs the container locally and the second on Amazon S3.  In the case of runOnBatchAndS3.sh this represents the first pass at making an interface for a GenePattern JobRunner class to call.

Usage on AWS Batch:

To run on Batch you need a few additional things set up on the Amazon side.  

1. A Batch compute environment (in the scripts above this is named 'TedTest', obviously not what we want for production)
2. A Batch Job definition referncing the container to be used and defining the command line necessary to execute the runS3OnBatch.sh script within the container (to mount directories and run the command line).  An example of this is provided in jobdef.json and it can be regisetered via the publishJobDef.sh script assuming you have your AWS CLI configured with a profile called 'genepattern'.  otherwise edit to suit your environment.
3. A published Docker container to be used in the job definition (in the examples its using liefeld/r313_cli)

This directory (for now) also includes the full source for the AffySTExpressionFileCreator module in src and some sample data in ./data that can be used for testing (e.g. with the runBatchAndS3 and runLocal scripts


Also note it will almost certainly fail running in local docker on a mac since it needs >2GB memory and on OSX (at the time of writing) containers are capped at 2GB




