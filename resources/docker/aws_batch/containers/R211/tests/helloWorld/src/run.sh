#!/bin/bash


#
# assign filenames for STDOUT and STDERR if not already set

# run the module
export RHOME=/packages/R-2.7.2/
export RFLAGS='--no-save --quiet --slave --no-restore'

java -cp /build -DR_HOME=$RHOME -Dr_flags="$RFLAGS" RunR hello.R hello 

