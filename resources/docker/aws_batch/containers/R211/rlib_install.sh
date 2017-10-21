#!/bin/sh

#Biobase_2.8.0
#robustbase_0.5-0-1
#mvtnorm_0.9-92
#pcaPP_1.8-2
#rrcov_1.0-00
#mvoutlier_1.4
#rpanel_1.0-5
#ks_1.6.13
#feature_1.2.4
#graph_1.26.0
#flowCore_1.14.1
#limma_3.4.5
#marray_1.26.0

export RLIB=/patches/rlib/2.11/site-library

cd /patches/rlib


wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/Biobase_2.8.0/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB Biobase_2.8.0.tar.gz
rm Biobase* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/robustbase_0.5-0-1/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB robustbase_0.5-0-1.tar.gz
rm robustbase* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/mvtnorm_0.9-92/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB mvtnorm_0.9-92.tar.gz
rm mvtnorm* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/pcaPP_1.8-2/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB pcaPP_1.8-2.tar.gz
rm pca* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/rrcov_1.0-00/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB rrcov_1.0-00.tar.gz
rm rrco* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/mvoutlier_1.4/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB mvoutlier_1.4.tar.gz
rm mvoutlier* installRLibrary.xml manifest plugin.zip installLibrary.R

#apt-get install tcl -y
#apt-get install tk -y
wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/rpanel_1.0-5/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB rpanel_1.0-5.tar.gz
rm rpanel* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/ks_1.6.13/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB ks_1.6.13.tar.gz
rm ks* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/feature_1.2.4/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB feature_1.2.4.tar.gz
rm feature* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/graph_1.26.0/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB graph_1.26.0.tar.gz 
rm graph* installRLibrary.xml manifest plugin.zip installLibrary.R 

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/flowCore_1.14.1/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB flowCore_1.14.1.tar.gz
rm flowC* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/limma_3.4.5/plugin.zip
unzip plugin.zip
R CMD INSTALL -l $RLIB limma_3.4.5.tar.gz
rm limma* installRLibrary.xml manifest plugin.zip installLibrary.R

wget  ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/2.11/marray_1.26.0/plugin.zip
unzip -o plugin.zip
R CMD INSTALL -l $RLIB marray_1.26.0.tar.gz
rm marray*  manifest plugin.zip 
