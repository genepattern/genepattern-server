#
# Look here for a list of available R Library plugins:
#     ftp://ftp.broadinstitute.org/pub/genepattern/plugins/rlib/
#
# http://stat.ethz.ch/R-manual/R-patched/library/utils/html/install.packages.html
#
load.packages <- function (libdir, patch.dir, server.dir, r.version)
{
    r.packages <- read.table(paste(libdir,"r.libraries.txt",sep=""))
    r.packages <- unlist(r.packages)

    # libraries are stored relative to the genepattern 'patches' directory
    if(!file.exists(patch.dir)) 
    {
        patch.dir <- paste(server.dir, patch.dir, sep="/")
    }
    if(!file.exists(patch.dir))
    {
        stop(paste("The path", patch.dir, "does not exist"))
    }

    cat("\nPatch dir: ",patch.dir);

    parent.dir <- paste(patch.dir, "rlib", r.version, sep="/")
    site.library <- paste(parent.dir, "site-library", sep="/")

    cat("\nLibrary dir: ",site.library)
    .libPaths(site.library)

    for(i in 1:length(r.packages)) {
        r.lib.spec <- toString(r.packages[[i]])
        r.lib.name    <- strsplit(r.lib.spec, "_")[[1]][[1]]
        r.lib.version <- strsplit(r.lib.spec, "_")[[1]][[2]]

        cat("\nLoading library: ") 
        print(r.lib.name, max.levels=0)

        if(!file.exists(site.library)) {

            print("A")


            stop(paste("The location at", site.library, "does not exist"))
        }
        print("Sit lib is ")
        print(site.library) 
        suppressPackageStartupMessages(library(r.lib.name, lib.loc = site.library, character.only=TRUE))
    }

    cat("\nDone loading R libraries.\n")
}
