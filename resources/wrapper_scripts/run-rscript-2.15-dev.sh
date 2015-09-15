#!/usr/bin/env bash
#
# Wrapper script for Rscript 2.15: development edition for working with
# GP-specific Renviron & Rprofile.  This one is meant for use with modules
# that run normal analyses.
#
# First initialize the environment via the 'use R-2.15' command
# then run Rscript passing along all the command line args
#

. /broad/tools/scripts/useuse
use R-2.15

# Now, modify the environment variables that affect the R environment
export R_ENVIRON=/xchip/gpdev/servers/genepatterntest/gp/resources/R215_Environ
export R_PROFILE=/xchip/gpdev/servers/genepatterntest/gp/resources/R215_Profile

# For per-user, could do it this way, assuming the GP user was passed in to the script somehow
# (this is pseudo-code, not real)
# Either of the arms of this branch will also turn off loading from implied locations
# such as ~/Library/R/2.15, ~/.Renviron or ${cwd}/.Renviron  
#
# export GP_HOME=/xchip/gpdev/servers/genepatterntest/gp/resources
#if [ -f $GP_HOME/resources/user_profiles/$GP_USER/R215_Environ ]; then
#   export R_ENVIRON_USER=$GP_HOME/resources/profiles/$GP_USER/R215_Environ
#else
#   # Forcibly ignore any user-level environ file.
#    R_ENVIRON_USER=
#fi
#... and likewise for _PROFILE

# For now, just do this...
# Forcibly ignore any user-level environ file and profile.
export R_ENVIRON_USER=
export R_PROFILE_USER=

Rscript "$@"
