Wrapper scripts for initializing the runtime environment for GenePattern modules.

Given a GenePattern module commandLine, e.g.
    commandLine=<python_2.6> ...

A common wrapper script will be called at runtime on the compute node, e.g.
    <wrapper-scripts>/run-with-env.sh -u Python-2.6 ...
    
Configuration involves (1) setting custom.properties, (2) defining site customization, and
(3) configuring Rscript.

========================================
(1) Set custom.properties
========================================
To use the wrapper scripts you must merge the included ./wrapper.properties 
into the <resources>/custom.properties file. There are two options for editing custom.properties:

Option 1: with the server shut down. Make edits to the custom.properties file and restart the server.
Option 2: with the server running, manually edit/update each property from the Admin > Server Settings > Custom page.

For a new install you can copy the wrapper.properties file into resources directory.
    cp -i wrapper.properties ../custom.properties

For an updated install, don't replace the custom.properties file if it already exists. Instead you must
merge the ./wrapper.properties into the ../custom.properties file. You must do this by hand.
(beta) There is an ant script, ./R/template/build.xml, which automatically merges
the two properties files.

========================================
(2a) Site customization
========================================
The wrapper scripts are generic enough that they will run on different runtime environments,
for example a local GP server running MacOS X, an HPC server with DotKit (http://sourceforge.net/projects/dotkit/), 
an HPC server with the Environment Modules (http://modules.sourceforge.net/). 

You should not edit the 'run-with-env.sh', 'run-rscript.sh' (or any other run-*.sh) shell scripts.
You also should not edit the 'env-default.sh', 'env-hashmap.sh', or 'env-lookup.sh' shell scripts.
It is possible that future updates to the GP server will replace these static scripts with newer versions.


There are two options for site customization.

* Option 1: create a new 'env-custom.sh' file. The GP server will automatically detect this as the site customization
script. Recommendation,
    cp -i env-default.sh env-custom.sh
Make edits to the env-custom.sh file.

* Option 2: set the 'env-custom' property to point to one of the built-in site customization scripts. 
E.g. edit custom.properties
    env-custom=env-custom-macos.sh
    
There are a number of built-in site customizations:
- env-custom-macos.sh; works for standard MacOS X instance.

These files will also potentially be replaced when you install new versions of GP.
It is very likely that you will need to made edits to these files. Please coordinate
with the GP Team so that these edits are incorporated into the next release of GP.

Additional documentation is in the comments in the run-with-env.sh script.


================================================
(2b) The 'env-custom' site customization options
================================================

You can customize the way that each environment is loaded by

a) defining a custom initEnv() function.
b) adding/replacing site-specific mappings, e.g.
    #putValue <canonical-name> <site-specific-name>
    putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
    
    # optionally, map one canonical-name to a list of site-specific-names, e.g.
    putValue 'R-3.1' 'gcc/4.7.2, R/3.1'

See env-default.sh or one of the other built-in site customization
scripts for more details.

===========================================
(3) R Programming Language configuration
===========================================
For newer versions of R (2.15, 3.0, 3.1, and 3.2 as of this writing) the built-in wrapper scripts
add some important new feature to the GenePattern System:
* isolated location for installed R packages; The Rscript will install/load R packages in a custom site-library
for the GP server instance. This is located in <patches>/Library/R/<R.version>.
* automatic R package installation; For modules which include an 'r.package.info' file the Rscript will
install and/or validate required R packages before each run of the module.

The wrapper script automatically (by convention) sets the following environment variables for the Rscript command:
    R_LIBS=" # set to empty value
    R_LIBS_USER=' ' # set to space character value
    R_LIBS_SITE=<patches>/Library/R/<R.version>
    R_ENVIRON=<wrapper-scripts>/R/Renviron.gp.site
    R_ENVIRON_USER=<wrapper-scripts>/R/<R.version>/Renviron.gp.site
    R_PROFILE=<wrapper-scripts>/R/<R.version>/Rprofile.gp.site
    R_PROFILE_USER=<wrapper-scripts>/R/<R.version>/Rprofile.gp.custom

See the R Programming Language documentation for more details:
    https://stat.ethz.ch/R-manual/R-patched/library/base/html/EnvVar.html
    https://stat.ethz.ch/R-manual/R-patched/library/base/html/Startup.html

For a Mac OS X install you must manually copy Rprofile.example.macosx into each R/{R.version} folder. E.g.
    cd ./wrapper_scripts/R/
    cp Rprofile.example.macosx 2.15/Rprofile.gp.custom


The default settings should work. Read on of you need to customize or debug your
installation.

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
   
