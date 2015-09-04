Wrapper scripts for initializing runtime environment for GenePattern modules
These wrapper scripts allow for site-specific customizations with minumum effort

Given a GenePattern module commandLine, e.g.
    commandLine=<R2.15_Rscript> ...

A common wrapper script will be called at runtime on the node on the compute farm, e.g.
    <wrapper-scripts>/run-with-env.sh -u R-2.15 -u Java ...
    
Each -u <env-name> defines a canonical runtime environment required by the module.
You can customize how this environment is initialized by ....

a) defining a custom loadEnv() function in ...
b) define site-specific mappings in ...


The run-with-env.sh wrapper script automatically sources the env-load-custom.sh and env-lookup-custom.sh scripts if present.
Look at env-load-default.sh for an example implementation and documentation.
Look at env-lookup-default.sh for the list of known canonical runtime environment names.
<resources>/wrapper_scripts
    ./env-load.sh
    ./env-load-custom.sh
    ./env-lookup.sh
    ./env-lookup-custom.sh
    
