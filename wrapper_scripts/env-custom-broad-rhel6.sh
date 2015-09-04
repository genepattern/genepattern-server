#!/usr/bin/env bash

#
# site customizations for Broad hosted GP servers running RHEL6 nodes
#

putValue 'GCC' 'GCC-4.9'
putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
putValue 'Matlab-2013a-MCR' '.matlab_2013a_mcr'
putValue 'Python-2.5' 'Python-2.6'   ### <---- no Python 2.5 dotkit
putValue 'R-2.0'  '.genepattern-internal-2.0.1'
putValue 'R-2.5'  '.genepattern-internal-2.5.1'
putValue 'R-2.7'  '.genepattern-internal-2.7.2'
putValue 'R-2.11' '.genepattern-internal-2.11.0'
