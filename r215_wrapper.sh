#!/usr/bin/env bash
export GP_HOME=/Users/eby/.genepattern
export R_ENVIRON=$GP_HOME/resources/R215_Environ
export R_PROFILE=$GP_HOME/resources/R215_Profile

# For per-user, could do it this way, assuming the GP user was passed in to the script somehow
# (this is pseudo-code, not real)
# Either of the arms of this branch will also turn off loading from implied locations
# such as ~/Library/R/2.15, ~/.Renviron or ${cwd}/.Renviron  
#if [ -f $GP_HOME/resources/user_profiles/$GP_USER/R215_Environ ]; then
#   export R_ENVIRON_USER=$GP_HOME/resources/profiles/$GP_USER/R215_Environ
#else
#   # Forcibly ignore any user-level environ file.
#   export R_ENVIRON_USER=
#fi
#... and likewise for _PROFILE

# For now, just do this...
# Forcibly ignore any user-level environ file and profile.
export R_ENVIRON_USER=
export R_PROFILE_USER=

/Library/Frameworks/R.framework/Versions/2.15/Resources/bin/Rscript "${@}"