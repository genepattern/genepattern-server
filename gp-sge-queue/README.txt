GenePattern Sun Grid Engine Integration Plugin
===============================================
This project includes source code and scripts necessary to build the SGE plugin for GenePattern.
This plugin uses source code developed by the Zamboni team at the Broad Institute.
Special thanks to Alec Wysoker who assisted with the implementation of this plugin
and the rest of the team who developed the code in Zamboni on which it is based: 
Ben Weisburd, Jonathan Burke, Kathleen Tibbetts, Tim Fennell, and Alec Wysoker.


========================================
Table of Contents:
1. Using the SGE plugin
2. Building / modifying the plugin
========================================


----------------------------------------
1. Using the SGE plugin
----------------------------------------
The GP server installer includes a pre-built jar file gp-sge-queue_<scala_version>.jar.
If you want to use the default build, skip ahead to step 7.

----------------------------------------
2. Building / modifying the plugin
----------------------------------------
Project dependencies:
* requires an installation of GenePattern Server. Tested with GP 3.7.3-beta; Byte compatible with GP 3.3.2-production.
  This project includes a copy of the GP 3.3.2 library (gp-full.jar, renamed to gp-full-3.3.2.prod.jar). 
  To use a different version of GenePattern, replace gp-full-3.3.2.prod.jar with the gp-full.jar in the WEB-INF/lib folder
  of your deployed GenePattern Server. You can also, as tested from Eclipse, simply link this project to the GenePattern project.
* requires Java 6 to be compatible with GP 3.3.3 and later.
* requires scala compiler to build the project. Built with SBT (scala build tool) 0.10.0 and scala-2.8.1. Also seems to work with scala-2.9.
  See http://www.scala-sbt.org/.
* requires scala-library.jar to deploy.


How to deploy this project to your GP server
--------------------------------------------
# Compile and package the library
(Hint: I install sbt here, /Users/Shared/Broad/Scala/sbt/sbt.sh)
sbt compile
sbt package

# deploy jar files to your GP server
4) deploy
Copy the target jar file(s) and dependent jar file(s) to the WEB-INF/lib folder of your GP server. 
Dependencies include:
./lib/sge-drmaa.jar
scala-library.jar
./target/scala-<version>/gp-sge-queue_<scala-version>.jar

# [not needed for GP >= 3.6.0] configure the DB
5) create the tables defined in the src/main/resources/sge_schema_oracle.sql script. 
   You may need to copy and then modify this script to work with a different database vendor.

6) Update the hibernate config file in your installed server. Add the following to WEB-INF/classes/hibernate.cfg.xml:
       <mapping resource="org/genepattern/server/executor/sge/JobSge.hbm.xml" />  

# configure your GP server
7) Edit the config file for your GP server. The path to the file is defined in genepattern.properties. To double-check, run this command:
grep config.file= genepattern.properties
Then edit that file, by adding the following to the executors section, replacing with relevant values for your installation.
The GP server does not automatically find these values from your environment. You need to manually edit the config file to set them.
Alternatively, you could try passing values directly with, for example -DSGE_ROOT=/broad/gridengine, on startup of your GP server.

executors:
    # [Experimental] SGE executor
    SGE:
        classname: org.genepattern.server.executor.sge.SgeCommandExecutor
        configuration.properties:
            SGE_ROOT: /broad/gridengine
            SGE_CELL: broad
            # the session file, when a relative path is used, is relative to the resources directory of your GP server
            SGE_SESSION_FILE: ./sge/sge_contact.txt
            SGE_BATCH_SYSTEM_NAME: gpdev_server
        # additional properties
        #default.properties:
        #    sge.priority:
        #    sge.queueName:
        #    sge.exclusive:
        #    sge.maxRunningTime:
        #    sge.memoryReservation:
        #    sge.maxMemory:
        #    sge.slotReservation:
        #    sge.restartable:
            
