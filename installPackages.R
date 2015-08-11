## The Broad Institute
## SOFTWARE COPYRIGHT NOTICE AGREEMENT
## This software and its documentation are copyright (2015) by the
## Broad Institute/Massachusetts Institute of Technology. All rights are
## reserved.
##
## This software is supplied without any warranty or guaranteed support
## whatsoever. Neither the Broad Institute nor MIT can be responsible for its
## use, misuse, or functionality.

main <- function() {
   # Parse command line to get the list of packages.  Note, can't use optparse for bootstrap reasons
   # (may be trying to install it), but it's not really needed anyway.
   cmd_args <- commandArgs(TRUE)
   r.package.info <- cmd_args[1]

   # Tracks whether or not the installation proceeds cleanly 
   installedCleanly <- TRUE

   isMac <- Sys.info()[["sysname"]]=="Darwin"

   # While we don't formally support Windows, we'll do our best to accommodate unless we start running into problems.
   isWindows <- Sys.info()[["sysname"]]=="Windows"

   # Read package info: load 4 column file
   # first column: package name in usual R style
   # second column: archive name or URL for source package.  Valid archive names are CRAN & BIOC.
   # third column: optional URL to Mac binary package location, or archive name in case source URL 
   #               is needed for Linux but not Mac (e.g. RSQLite on R 2.15)
   # fourth column: optional URL to Windows binary package location, or archive name in case source URL 
   #               is needed for Linux but not Windows (e.g. RSQLite on R 2.15)
   # NOTE: Columns 3 & 4 can be blank if a archive name is present in column 2.
   pkg.info <- read.table(r.package.info, fill=TRUE, header=TRUE, sep="\t",
                          col.names=c("package", "src_archive_name", "Mac_archive_name", "Windows_archive_name"),
                          colClasses=c("character", "character", "character", "character"))

   # Index to look up the archive/URL.
   archiveLookupIndex <- 2
   if (isMac) archiveLookupIndex <- 3
   if (isWindows) archiveLookupIndex <- 4
   
   # Determine the list of GP library locations where we check for installed packages.  We are 
   # pointedly ignoring anything installed in .Library (the "base" R library location) but only
   # looking at GP-installed paths instead.
   gp.lib.loc <- .libPaths()[! .libPaths() %in% .Library ]
   write("\n---------------------", stdout())
   write(paste0("gp.lib.loc: ", gp.lib.loc), stdout()) 
   write("---------------------\n", stdout())

   # Process the list of packages.
   pkgs.to.install <- character()
   for (i in 1:NROW(pkg.info)) {
      pkg.name <- pkg.info[i, 1]
      pkg.archive_or_URL <- pkg.info[i, archiveLookupIndex]

      # First, we'll go through several guard clauses to make sure we can/should process this package
      
      # Skip (but note!) any rows where pkg.name has not been provided
      if (is.null.or.blank(pkg.name)) {
         write("\n---------------------", stderr())
         write(paste0("Skipping row with no package specified, on or around line ", i, "."), stderr())
         write("---------------------\n", stderr())
         installedCleanly <- FALSE
         next
      }

      # Check whether the package is already installed; if so, skip it.  This is not considered an error.
      if (pkg.name %in% installed.packages(lib.loc=gp.lib.loc)[,"Package"]) {
         write("\n---------------------", stdout())
         write(paste0("Skipping ", pkg.name, " as it is already installed"), stdout()) 
         write("---------------------\n", stdout())
         next
      }
      
      # Skip any packages with no archive or URL specified (or specified as SKIP).  This is not considered an error.  
      if (is.null.or.blank(pkg.archive_or_URL) || pkg.archive_or_URL == 'SKIP') {
         write("\n---------------------", stdout())
         write(paste0("Package '", pkg.name, "' has no archive/URL for this platform.  Skipping."), stdout())
         write("---------------------\n", stdout())
         next
      }
      
      # After those checks we're ready to process the package. 
      # If the package source is a archive then queue it up for installation via standard R mechanisms.
      if (is.archiveLabel(pkg.archive_or_URL)) {
         write("\n---------------------", stdout())
         write(paste0("Queueing package ", pkg.name, " for installation"), stdout())
         write("---------------------\n", stdout())
         pkgs.to.install <- append(pkgs.to.install, pkg.name)
      } else {
         # Otherwise it should be a URL of a package to be downloaded and installed directly.  First,
         # install any packages previously queued as they may be dependencies necessary for this one.
         if (!install.queued.pkgs(pkgs.to.install, gp.lib.loc)) installedCleanly <- FALSE
         pkgs.to.install <- character()

         if (!download.and.install.from.URL(pkg.name, pkg.archive_or_URL, gp.lib.loc)) installedCleanly <- FALSE
      }
   }
   
   # Install any remaining queued packages after the loop is complete.
   if (!install.queued.pkgs(pkgs.to.install, gp.lib.loc)) installedCleanly <- FALSE
   
   if (installedCleanly) {
      quit(save="no", status=0)
   } else {
      stop("One or more errors occurred during package installation.  Check logs for more details")
   }
}

## Helper functions ##

is.archiveLabel <- function(value) {
   return(value == 'CRAN' || value == 'BIOC')
}

is.null.or.blank <- function(value) {
   return (is.null(value) || is.na(value) || grepl("^[[:space:]]*$", value))
}

# Load the BiocInstaller if it's not already present
init.bioc.loader <- function(gp.lib.loc) {
   # Don't init if the package has already been loaded
   if (exists("biocLite")) return(TRUE)

   # Don't download the BiocInstaller if we already have it locally.  We suppress warnings here because we'll respond
   # to the issue by installing the BiocInstaller and so it's not really an error condition.
   suppressWarnings(
      if (require(BiocInstaller, lib.loc=gp.lib.loc, quietly=TRUE)) return(TRUE)
   )
   
   # Needed for the Bioconductor loader script when run via Rscript.  They've fixed this, but we're keeping it just in
   # case there is a reversion (no harm in doing so).  It's OK to load this from the R system location.
   library(methods, quietly=TRUE, verbose=FALSE)

   # Errors should not be let through to the default handler as that would stop the script outright.  Instead, we want to
   # flag the issue but allow the script to continue to try to install any other packages that it can.  In all probability, 
   # an error here will be fatal outright anyway as our installer itself could not be installed.  We do it this way for
   # consistency, however.  It may allow any URL-specified packages to be installed despite the errors, for example.
   tryCatch({
         write("\n---------------------", stdout())
         write("About to initialize the Bioconductor loader", stdout())
         write("---------------------\n", stdout())
         source("http://bioconductor.org/biocLite.R")
      },
      error=function(err) {
         write(paste0("Error: ", conditionMessage(err)), stderr())
         return(FALSE)
      })
      return(verify.package.installed("BiocInstaller", gp.lib.loc))
}

verify.package.installed <- function(pkg.name, gp.lib.loc) {
   # Check whether the package actually installed.
   if (!require(pkg.name, lib.loc=gp.lib.loc, character.only=TRUE, quietly=FALSE)) {
      write("\n---------------------", stderr())
      write(paste0("Package '", pkg.name, "' failed to install cleanly!"), stderr())
      write("---------------------\n", stderr())
      return(FALSE)
   } else {
      write("\n---------------------", stdout())
      write(paste0("Package '", pkg.name, "' installed"), stdout())
      write("---------------------\n", stdout())
      return(TRUE)
   }
}

install.queued.pkgs <- function(pkgs.to.install, gp.lib.loc) {
   if (NROW(pkgs.to.install) == 0) return(TRUE)
   if (!init.bioc.loader(gp.lib.loc)) return(FALSE)

   # Likewise, we don't want a failure here to stop the whole script.  Note that we're using the Bioconductor installer for
   # CRAN packages as well as BIOC since it sorts these out and hands them off to install.packages automatically.  Also,
   # when working with a batch list, a single failure will not halt the entire batch.
   tryCatch({
         biocLite(pkgs.to.install, suppressUpdates=TRUE, suppressAutoUpdate=TRUE, dependencies=FALSE)
      },
      error=function(err) {
         write(paste0("Error: ", conditionMessage(err)), stderr())
         return(FALSE)
      })

   # We want to check *all* the packages in the list and not stop if one of them fails.  This is to make sure we get all
   # the failure messages related to these packages.
   installedCleanly <- TRUE
   for (pkg.name in pkgs.to.install) {
      if (!verify.package.installed(pkg.name, gp.lib.loc)) installedCleanly <- FALSE
   }
   return(installedCleanly)
}

# Download and install a single package from a URL.
download.and.install.from.URL <- function(pkg.name, pkg.URL, gp.lib.loc) {
   pkg.filename <- basename(pkg.URL)
   download.file(pkg.URL, pkg.filename, quiet=TRUE)
   on.exit(unlink(pkg.filename))
   
   # As above, for the same reasons.
   tryCatch({
         install.packages(pkg.filename, repos=NULL, quiet=TRUE)
         return(verify.package.installed(pkg.name, gp.lib.loc))
      },
      error=function(err){
         write(paste0("Error: ", conditionMessage(err)), stderr())
         return(FALSE)
      })
}

# Execute the main function
main()
