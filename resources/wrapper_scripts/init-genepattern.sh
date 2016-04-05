#!/usr/bin/env bash

##############################################################################
#
# Initialize the environment for the ./Tomcat/catalina-macapp.sh command
#
# Environment variables:
#     GENEPATTERN_HOME
#     CATALINA_HOME
##############################################################################

#
# Must set GENEPATTERN_HOME to the fully qualified path to the data directory
# Default location is '../../../gp_home', relative to wrapper scripts folder.
#
if [ -z "$GENEPATTERN_HOME" ]; then
    export GENEPATTERN_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}")"; cd ../; cd ../; cd ../; cd gp_home; pwd)
    echo "setting GENEPATTERN_HOME to ${GENEPATTERN_HOME}";
else
    echo "GENEPATTERN_HOME=${GENEPATTERN_HOME}"
fi

#
# Must set CATALINA_HOME to the root of your Tomcat installation
#
if [ -z "$CATALINA_HOME" ]; then
    export CATALINA_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}")"; cd ../; cd ../; cd ../; cd gp/; cd Tomcat/; pwd)
    echo "setting CATALINA_HOME to ${CATALINA_HOME}";
else
    echo "CATALINA_HOME=${CATALINA_HOME}";
fi

export JAVA_OPTS=-Xmx1024m
