#!/bin/bash
#
# First argument is server working dir, second arg is server resources dir 
#
# Note that file paths (esp resources) will need to be customized for any non-vanilla 
# GenePattern install (ie dev environments, cloud, beta, etc)
java -classpath ../../website/WEB-INF/classes:../../website/WEB-INF/lib/* org.genepattern.server.purger.impl02.Purger02 ../../tmp   ../../resources



