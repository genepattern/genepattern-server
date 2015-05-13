#!/usr/bin/env bash

#
# Site specific customizations for wrapper environment for GenePattern Server
# installation at Indiana University
#

#declare -a envCmd
envCmd=("module" "load")
#debug: envCmd=("echo" "loading module")

#declare -A envMap
envMap=( 
  ['Java']='java/1.7.0_51'
  ['Java-7']='java/1.7.0_51'
  ['.matlab-2013a']='matlab/2013a'
  ['Python-2.5']='python/2.5.4'
  ['Python-2.6']='python/2.6.9'
  ['R-2.7']='R/2.7.2'
  ['R-2.10']='R/2.10.1'
  ['R-2.11']='R/2.11.1'
  ['R-2.13']='R/2.13.2'
  ['R-2.14']='R/2.14.2'
  ['R-2.15']='R/2.15.2'
  ['R-3.0']='R/3.0.1'
  ['R-3.1']='R/3.1.1'
  ['GCC']='gcc/4.7.2'
)

# Note to self, make sure to set R2.14_HOME with the correct path

