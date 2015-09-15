Wrapper scripts for initializing the runtime environment for GenePattern modules.

Given a GenePattern module commandLine, e.g.
    commandLine=<R2.15_Rscript> ...

A common wrapper script will be called at runtime on the compute node, e.g.
    <wrapper-scripts>/run-with-env.sh -u R-2.15 ...

========================================
Edit <resources>/custom.properties file
==================================    
You must first add entries to the <resources>/custom.properties file of your server. Start by looking at
the wrapper.custom.properties file. Most of these entries will work for all servers. A few require 
site specific customization.

Option 1: with the server shut down. Make edits to the custom.properties file and restart the server.
Option 2: with the server running, manually edit/update each property from the Admin > Server Settings > Custom page.


========================================
Create or edit the ./env-custom.sh script
========================================

For detailed documentation look at the comments in the run-with-env.sh script.

You can customize the way that each environment is loaded by

a) defining a custom loadEnv() function.
b) adding/replacing site-specific mappings, e.g.
    #putValue <canonical-name> <site-specific-name>
    putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
    
    # optionally, map one canonical-name to a list of site-specific-names, e.g.
    putValue 'R-3.1' 'gcc/4.7.2, R/3.1'


===========================================
R Programming Language module configuration
===========================================
The default settings should work. Read on of you need to customize or debug your
installation. As of this writing these flags apply for R-2.14, R-2.15, R-3.0, and R-3.1 modules.

R_LIBS_SITE
    Sets the site-library location for installing R packages. This is the default location where 
modules will look when loading R packages. This is passed along to the wrapper script via the -e flag, e.g.
    run-with-env.sh ... '-e' 'R_LIBS_SITE=<patches>/Library/R/2.15' ...
GenePattern also uses this location for installing required packages.

R_LIBS_USER
    For user-level libraries (if desired).  Not here; set in the R_ENVIRON_USER
for that particular user.  Just adding it here as a note.

R_LIBS
    By default, GenePattern forcibly ignores the R_LIBS setting. Both the Broad DotKit and the IU Module 
add an unwritable system directory here that interferes with installation.  Besides, we don't want to load 
packages from areas we don't control (other than .Library).  This will also disable loading settings from 
implied locations (e.g. ~/.Renviron, etc.).

# Additional proxy settings 
ftp_proxy=
ftp_proxy_user=
ftp_proxy_password=
http_proxy=
no_proxy=
   