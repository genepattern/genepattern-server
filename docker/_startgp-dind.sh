#/bin/bash
dockerd & 
export GENEPATTERN_HOME=$PWD
/opt/genepattern/StartGenePatternServer
