##
## Copyright (c) 2017 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
##

local({
   main <- function() {   
      # Parse command line to get the list of packages.  Note, can't use optparse for bootstrap reasons
      # (may be trying to install it), but it's not really needed anyway.
      cmd_args <- commandArgs(TRUE)
      
      # Require the at least the r.package.info file; allow output.file as an optional arg
      if (NROW(cmd_args) < 1 || NROW(cmd_args) > 2 || grepl('^[:space:]*$', cmd_args[1])) {
         stop("Usage: Rscript InstallPackages r.package.info [output.file]")
      }
      r.package.info <- cmd_args[1]
      
      # Check for empty/missing output.file arg
      if (NROW(cmd_args) < 2 || is.null(cmd_args[2]) || grepl('^[:space:]*$', cmd_args[2])) {
         output.file <- ".report.log"
      } else {
         output.file <- cmd_args[2]
      }
   
      # Tracks whether or not the installation proceeds cleanly 
      installedCleanly <- TRUE
   
      isMac <- Sys.info()[["sysname"]]=="Darwin"
   
      # While we don't formally support Windows, we'll do our best to accommodate unless we start running into problems.
      isWindows <- Sys.info()[["sysname"]]=="Windows"
   
      isSrcInstall <- !isMac && !isWindows

      # Is there a CRAN mirror defined?  If not, use the default one. This used for building CRAN URLs.
      cran.repos.url <- getOption("repos")["CRAN"]
      if (!is.usable.URL(cran.repos.url)) {
         cran.repos.url <- "http://cran.r-project.org"
      }
      
      # Add a trailing '/' if the mirror URL does not have it already.
      if (!grepl('/$', cran.repos.url)) { cran.repos.url <- paste0(cran.repos.url, '/') }
   
      # Read package info list: load 6 column file
      # first column: required package name in usual R style
      # second column: optional CRAN/BIOC-style version string.
      # third column: optional archive name or URL for source package.  The archive name 'CRAN' has a special meaning,
      #               but anything else serves as documentation only (see Precedence Rules below).
      # fourth column: optional URL for a source package location
      # fifth column: optional URL for a Mac binary package location
      # sixth column: optional URL for a Windows binary package location
      # Precedence Rules:
      # 1) If a URL is specified in the column appropriate for the current platform, that will be used for installation
      #    regardless of any other specifications.  This must be usable by R's download.file (http or ftp only), 
      #    otherwise it is ignored.
      # 2) Otherwise, if no such URL is specified but (a) the CRAN archive name AND (b) a version string are specified AND
      #    (c) we're installing from source (i.e. on Linux), then a CRAN URL will be constructed and used for installation.
      #    NOTE: there are two possible URL conventions, one for the Archived packages and one for the Current packages.  
      #    Both URLs are built and tried in turn, starting with the Archived URL since pinned packages in general will not 
      #    be Current.
      # 3) Otherwise, the package name will be fed into the biocLite installer method which will try to install it from
      #    either CRAN or Bioconductor as appropriate.  In this case, all other info on the line is considered to be
      #    "informational-only", that is for documentation purposes.
      #    Future revisions may more strongly enforce version info but that is not defined or implemented at this time.
      pkg.info.list <- read.table(r.package.info, fill=TRUE, header=TRUE, quote="", sep=",", strip.white=TRUE,
                                 col.names=c("package", "requested_version", "archive_name", "src_URL", "Mac_URL", "Windows_URL"),
                                 colClasses=c("character", "character", "character", "character", "character", "character"))

      # Index to look up the URL.
      urlLookupIndex <- 4
      if (isMac) urlLookupIndex <- 5
      if (isWindows) urlLookupIndex <- 6
      
      # Determine the list of GP library locations where we check for installed packages.  We are 
      # pointedly ignoring anything installed in .Library (the "base" R library location) but only
      # looking at GP-installed paths instead. Be sure to normalize all paths to take care of any
      # relative references or symlinks.
      norm.Library <- normalizePath(.Library)
      norm.libPaths <- normalizePath(.libPaths())
      gp.lib.loc <- norm.libPaths[! norm.libPaths %in% norm.Library ]
      write("\n---------------------", stdout())
      write(paste0("gp.lib.loc: ", gp.lib.loc), stdout()) 
      write("---------------------\n", stdout())

      # Process the list of packages.
      for (i in 1:NROW(pkg.info.list)) {
         # First, we'll go through several guard clauses to make sure we can/should process this line
         
         # Skip any totally blank lines
         if (all(apply(pkg.info.list[i,], MARGIN=2, FUN=function (x) is.null.or.blank(x) ))) {
            write("\n---------------------", stdout())
            write(paste0("Skipping blank row, on or around line ", i, "."), stdout())
            write("---------------------\n", stdout())
            next
         }

         # Create a struct with named attributes to carry the important package-related info during processing.
         # We're attaching these attribs to a NULL to avoid any extra overhead.
         pkg.info <- NULL
         pkg.info$name <- pkg.info.list[i, 1]
         pkg.info$URL <- pkg.info.list[i, urlLookupIndex]
         pkg.info$altURL <- NULL
         pkg.info$do.byName.install <- TRUE   # Default; will determine before install...
         pkg.info$requested.version <- pkg.info.list[i, 2]
         pkg.info$archive <- pkg.info.list[i, 3]
         pkg.info$new.install <- FALSE
         pkg.info$failed <- FALSE
         
         # Skip (but note!) any rows where the package name has not been provided but other fields are present.
         if (is.null.or.blank(pkg.info$name)) {
            write("\n---------------------", stderr())
            write(paste0("Error: Skipping row with no package specified, on or around line ", i, "."), stderr())
            write("---------------------\n", stderr())
            installedCleanly <- FALSE
            next
         }

         # Check whether the package is already installed; if so, skip it.  This is not considered an error.
         if (pkg.info$name %in% installed.packages(lib.loc=gp.lib.loc)[,"Package"]) {
            write("\n---------------------", stdout())
            write(paste0("Skipping ", pkg.info$name, " as it is already installed"), stdout()) 
            write("---------------------\n", stdout())
            report.pkg.info(pkg.info, output.file)
            next
         }

         # Create CRAN URLs if needed and also determine whether to install byName or from URL.
         pkg.info <- build.CRAN.URLs.if.needed(pkg.info, cran.repos.url, isSrcInstall)
         if (pkg.info$do.byName.install) {
            pkg.info <- install.named.package(pkg.info, gp.lib.loc, output.file)
         } else {
            pkg.info <- download.and.install.from.URL(pkg.info, gp.lib.loc)
         }

         if (pkg.info$failed) installedCleanly <- FALSE
         report.pkg.info(pkg.info, output.file)
      }
      
      if (installedCleanly) {
         quit(save="no", status=0)
      } else {
         stop("One or more errors occurred during package installation.  Check logs for more details")
      }
   }
   
   ## Helper functions ##
   
   is.usable.URL <- function(URL) {
     return(is.character(URL) && grepl("^(http|ftp|file)://", URL))
   }
   
   is.null.or.blank <- function(value) {
      return (is.null(value) || is.na(value) || grepl("^[[:space:]]*$", value))
   }
   
   # Load the BiocInstaller if it's not already present
   init.bioc.loader <- function(gp.lib.loc, output.file) {
      # Don't init if the package has already been loaded
      if (exists("biocLite")) return(TRUE)
   
      # Don't download the BiocInstaller if we already have it locally.  We suppress warnings here because we'll respond
      # to the issue by installing the BiocInstaller so this is not really an error condition.
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
            # Always pull the init script from the main Bioconductor site for simplicity (rather than checking for bioC_mirror option).
            source("http://bioconductor.org/biocLite.R")
         },
         error=function(err) {
            write(paste0("Error: ", conditionMessage(err)), stderr())
            return(FALSE)
         })
         # Set-up a stub pkg.info object for BiocInstaller so we can add it to the report.
         bioc.pkg.info <- NULL
         bioc.pkg.info$name <- "BiocInstaller"
         bioc.pkg.info$requested.version <- ""
         bioc.pkg.info$new.install <- TRUE
         bioc.pkg.info$failed <- FALSE
         bioc.pkg.info <- verify.package.installed(bioc.pkg.info, gp.lib.loc)
         report.pkg.info(bioc.pkg.info, output.file)
         return(!bioc.pkg.info$failed)
   }
   
   verify.package.installed <- function(pkg.info, gp.lib.loc) {
      # Check whether the package actually installed.
      if (!require(pkg.info$name, lib.loc=gp.lib.loc, character.only=TRUE, quietly=FALSE)) {
         pkg.info$failed <- TRUE
         write("\n---------------------", stderr())
         write(paste0("Package '", pkg.info$name, "' failed to install cleanly!"), stderr())
         write("---------------------\n", stderr())
      } else {
         write("\n---------------------", stdout())
         write(paste0("Package '", pkg.info$name, "' installed"), stdout())
         write("---------------------\n", stdout())
      }
      return(pkg.info)
   }

   install.named.package  <- function(pkg.info, gp.lib.loc, output.file) {
      if (!init.bioc.loader(gp.lib.loc, output.file)) {
         pkg.info$failed <- TRUE
         return(pkg.info)
      }
   
      # Likewise, we don't want a failure here to stop the whole script.  Note that we're using the Bioconductor installer for
      # CRAN packages as well as BIOC since it sorts these out and hands them off to install.packages automatically.
      tryCatch({
            biocLite(pkg.info$name, suppressUpdates=TRUE, suppressAutoUpdate=TRUE, dependencies=FALSE)
            pkg.info <- verify.package.installed(pkg.info, gp.lib.loc)
            if (!pkg.info$failed) pkg.info$new.install <- TRUE
         },
         error=function(err) {
            write(paste0("Error: ", conditionMessage(err)), stderr())
            pkg.info$failed <- TRUE
         })

      return(pkg.info)
   }
   
   # Download a file from a URL to a given name, returning a simple TRUE or FALSE on success while 
   # suppressing any thrown errors
   download.from.URL <- function(URL, filename) {
      tryCatch({
            return (download.file(URL, filename, quiet=TRUE) == 0)
         },
         warning=function(w) {
            write(paste0("Warning: ", conditionMessage(w)), stdout())
         },
         error=function(err) {
            write(paste0("Error: ", conditionMessage(err)), stdout())
         })
         return(FALSE)
   }
   
   # Download and install a single package from a URL or an alternate location if the first is not available.
   download.and.install.from.URL <- function(pkg.info, gp.lib.loc) {
      pkg.filename <- basename(pkg.info$URL)
      on.exit(unlink(pkg.filename))
   
      write("\n---------------------", stdout())
      write(paste0("Attempting to download package '", pkg.info$name, "' from primary URL ", pkg.info$URL), stdout())
      write("---------------------\n", stdout())
      downloadSucceeded <- download.from.URL(pkg.info$URL, pkg.filename)

      if (downloadSucceeded) {
         # Suppressing errors as in install.named.package above, for the same reasons.
         tryCatch({
               install.packages(pkg.filename, repos=NULL)
               pkg.info <- verify.package.installed(pkg.info, gp.lib.loc)
               if (!pkg.info$failed) pkg.info$new.install <- TRUE 
            },
            error=function(err) {
               write(paste0("Error: ", conditionMessage(err)), stderr())
               pkg.info$failed <- TRUE
            })
      }

      # If the primary download failed or the package didn't install, try again with the altURL
      if ((!downloadSucceeded || pkg.info$failed) && !is.null.or.blank(pkg.info$altURL)) {
         write("\n---------------------", stdout())
         write(paste0("Attempting to download package '", pkg.info$name, "' from alternate URL ", pkg.info$altURL), stdout())
         write("---------------------\n", stdout())
         downloadSucceeded <- download.from.URL(pkg.info$altURL, pkg.filename)
      
         if (!downloadSucceeded) {
            pkg.info$failed <- TRUE
            return(pkg.info)
         }

         # Try again to install
         pkg.info$failed <- FALSE
         tryCatch({
               install.packages(pkg.filename, repos=NULL)
               pkg.info <- verify.package.installed(pkg.info, gp.lib.loc)
               if (!pkg.info$failed) pkg.info$new.install <- TRUE 
            },
            error=function(err) {
               write(paste0("Error: ", conditionMessage(err)), stderr())
               pkg.info$failed <- TRUE
            })
      }
   
      return(pkg.info)
   }

   build.CRAN.URLs.if.needed <- function(pkg.info, cran.repos.url, isSrcInstall) {
      # Checking Rule 1: is it a URL?  If so, return and use it as-is.
      if (is.usable.URL(pkg.info$URL)) {
         pkg.info$do.byName.install <- FALSE
         return(pkg.info)
      }
      
      # Checking Rule 2: is it a src install with CRAN and version specified?  If not, return and use it as-is.
      if (!isSrcInstall || is.null.or.blank(pkg.info$requested.version) ||  
          is.null.or.blank(pkg.info$archive) || pkg.info$archive != "CRAN") { 
         return(pkg.info)
      }
      
      # Otherwise, build CRAN URLs to source packages based on the general established path name patterns.
      pkg.info$do.byName.install <- FALSE
      
      # Create expected URL for a source package.  
      # Should look like e.g. http://cran.r-project.org/src/contrib/Archive/spatial/spatial_7.3-5.tar.gz
      pkg.info$URL <- paste0(cran.repos.url, "src/contrib/Archive/", pkg.info$name, "/", pkg.info$name, "_", pkg.info$requested.version, ".tar.gz")

      # Create expected URL for a source package.  
      # Should look like e.g. http://cran.r-project.org/src/contrib/spatial_7.3-5.tar.gz
      pkg.info$altURL <- paste0(cran.repos.url, "src/contrib/", pkg.info$name, "_", pkg.info$requested.version, ".tar.gz")
      return(pkg.info)
   }

   report.pkg.info <- function(pkg.info, output.file) {
      if (is.null.or.blank(pkg.info$requested.version)) {
         pkg.info$requested.version <- "Not specified"
      }

      if (pkg.info$failed) {
         pkg.version.actual <- ""
      } else {
         pkg.version.actual <- as.character(packageVersion(pkg.info$name))
      }
      
      write(sprintf("r_package_name=%s, requested_version=%s, version_actual=%s, new_install=%s, install_succeeded=%s",
                    pkg.info$name, pkg.info$requested.version, pkg.version.actual, pkg.info$new.install, !pkg.info$failed), 
            file=output.file, append=TRUE)
   }
   
   sessionInfo()
   
   # Execute the main function
   main()
})
