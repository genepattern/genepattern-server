#/usr/bin/bash

#graph_1.22.2
#Biobase_2.6.1
#robustbase_0.5-0-1
#mvtnorm_0.9-9
#pcaPP_1.7
#rrcov_1.0-00
#ks_1.6.8
#rpanel_1.0-6
#feature_1.2.4
#flowCore_1.12.0

export RLIB=/patches/rlib/2.10/site-library

cd /patches/rlib

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/graph_1.22.2/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB graph_1.22.2.tar.gz 
rm graph* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/Biobase_2.6.1/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB Biobase_2.6.1.tar.gz
rm Biobase* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/robustbase_0.5-0-1/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB robustbase_0.5-0-1.tar.gz
rm robustbase* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/mvtnorm_0.9-9/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB mvtnorm_0.9-9.tar.gz
rm mvtnorm* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/pcaPP_1.7/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB pcaPP_1.7.tar.gz
rm pca* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/rrcov_1.0-00/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB rrcov_1.0-00.tar.gz
rm rrco* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/ks_1.6.8/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB ks_1.6.8.tar.gz
rm ks* installRLibrary.xml manifest plugin.zip installLibrary.R

#wget https://cran.r-project.org/src/contrib/00Archive/tcltk/tcltk_0.1-1.tar.gz

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/rpanel_1.0-6/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB rpanel_1.0-6.tar.gz
#R CMD INSTALL rpanel_1.0-6.tar.gz
rm rpanel* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/feature_1.2.4/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB feature_1.2.4.tar.gz
rm feature* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.10/flowCore_1.12.0/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB flowCore_1.12.0.tar.gz
rm flowC* installRLibrary.xml manifest plugin.zip installLibrary.R

