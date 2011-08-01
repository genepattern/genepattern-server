GenePattern Sun Grid Engine Integration Plugin
===============================================
This project includes source code and build scripts necessary to build the SGE plugin for GenePattern.

How to deploy this project to your GP server
--------------------------------------------
# Compile and package the library
This library is implemented in both scala and java. It has been tested with Scala-2.8.1.
1) Download, install, and configure SBT (scala build tool).
2) Compile.
sbt compile
3) Package
sbt package

# deploy jar files to your GP server
4) deploy
Copy the target jar file(s) and dependent jar file(s) to the WEB-INF/lib folder of your GP server. 
The target file is created by the sbt package command. The depend libraries are in the lib folder. 
No need to duplicate library files which are part of a default GP installation. 
Don't copy the hibernate and gp-full jar files.

# configure the DB
5) make sure to create the tables defined in the sge_schema_oracle.sql script. (In the resources folder of this project).
6) Finally, you need to update the hibernate config file in your installed server, so that it loads the JobSge.hbm.xml file.
