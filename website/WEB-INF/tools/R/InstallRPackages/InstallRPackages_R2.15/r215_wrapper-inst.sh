#!/usr/bin/env bash
export GP_HOME=/Users/eby/.genepattern
export R_ENVIRON=$GP_HOME/resources/R215_Environ
export R_PROFILE=$GP_HOME/resources/R215_Profile

# Expect this to come in via e.g. the command line, then we "shift" it away before the final call
#export GP_USER=eby@broadinstitute.org
export GP_USER=SYSTEM

# For per-user, could do it this way, assuming the GP user was passed in to the script somehow
# Either of the arms of this branch will also turn off loading from implied locations
# such as ~/Library/R/2.15, ~/.Renviron or ${cwd}/.Renviron  
if [ -f ${GP_HOME}/resources/user_profiles/${GP_USER}/R215_Environ ]; then
   export R_ENVIRON_USER=${GP_HOME}/resources/user_profiles/${GP_USER}/R215_Environ
else
   # Forcibly ignore any user-level environ file.
   export R_ENVIRON_USER=
fi
if [ -f ${GP_HOME}/resources/user_profiles/${GP_USER}/R215_Profile ]; then
   export R_PROFILE_USER=${GP_HOME}/resources/user_profiles/${GP_USER}/R215_Profile
else
   # Forcibly ignore any user-level environ file.
   export R_PROFILE_USER=
fi

/Library/Frameworks/R.framework/Versions/2.15/Resources/bin/Rscript "${@}" 2>&1