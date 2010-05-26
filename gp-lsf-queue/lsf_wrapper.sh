#!/bin/bash                                                                                                            
#                                                                                                                      
# helper script to wrap calls to bsub so that stdout can be easily separated from the LSF report
#
"$@" >> my_stdout.txt
