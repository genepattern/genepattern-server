Wrapper scripts for initializing the runtime environment for GenePattern modules.
These wrapper scripts allow for site-specific customizations.

Given a GenePattern module commandLine, e.g.
    commandLine=<R2.15_Rscript> ...

A common wrapper script will be called at runtime on the compute node, e.g.
    <wrapper-scripts>/run-with-env.sh -u R-2.15 -u Java ...
    
Each '-u <env-name>' defines a canonical runtime environment required by the module.

You can customize how this environment is initialized by editing the env-custom.sh script:

a) defining a custom loadEnv() function.
b) adding/replacing site-specific mappings, e.g.
    #putValue <canonical-name> <site-specific-name>
    putValue 'Matlab-2010b-MCR' '.matlab_2010b_mcr'
    
    # optionally, map one canonical-name to a list of site-specific-names, e.g.
    putValue 'R-3.1' 'gcc/4.7.2, R/3.1'


The run-with-env.sh wrapper script automatically sources the env-load-custom.sh and env-lookup-custom.sh scripts if present.
Look at env-load-default.sh for an example implementation and documentation.
Look at env-lookup-default.sh for the list of known canonical runtime environment names.
<resources>/wrapper_scripts
    ./env-load.sh
    ./env-load-custom.sh
    ./env-lookup.sh
    ./env-lookup-custom.sh
    
