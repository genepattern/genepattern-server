## The Broad Institute
## SOFTWARE COPYRIGHT NOTICE AGREEMENT
## This software and its documentation are copyright (2014) by the
## Broad Institute/Massachusetts Institute of Technology. All rights are
## reserved.
##
## This software is supplied without any warranty or guaranteed support
## whatsoever. Neither the Broad Institute nor MIT can be responsible for its
## use, misuse, or functionality.

GP.affyst.efc <- function(files.to.process, normalize, background.correct, qc.plot.format,  
                         clm.file, annotate.probes, output.file.base) {
   # Check that we actually have files
   if (NROW(files.to.process) < 1) {
      stop("No CEL files were found in the input file(s)")
   }

   # Make sure that there are no name collisions (CEL files with duplicated names)
   check.for.dup.file(files.to.process)

   # Load the CLM file and rearrange files.to.process to match
   if (!is.null(clm.file)) {
      print(paste0("Rearranging according to CLM file ", clm.file))
      clm <- read.clm(clm.file)
      rearranged <- rearrange.files(files.to.process, clm)
      files.to.process <- rearranged$file.list
      clm <- rearranged$clm
      write.cls.from.clm(clm, output.file.base)
   }
   else {
      clm <- NULL
   }

   # Get the arrayTypeNames of all files and use the first as the reference.
   arrayTypeNames <- sapply(files.to.process, getCelChipType)
   arrayTypeName <- arrayTypeNames[1]

   # Make sure that this is an ST array
   if (!haveSTArrayType(arrayTypeName)) {
      stop(paste0("Array type ", arrayTypeName, 
          " does not seem to be an ST array.  You may want to check whether it is supported by ExpressionFileCreator instead."))
   }
   
   # Make sure all files match the first.  The read.celfiles() call will also do this, but the error message
   # it uses is not very clear as we need to set verbose=FALSE on that call.  The following replicates that
   # verbose=TRUE error message (based on 'checkChipTypes' in oligo's utils-general.R).
   if (length(unique(arrayTypeNames)) != 1) stop("All the CEL files must be of the same type.")
   print(paste0("Processing files of array type ", arrayTypeName))
   
   # The read.celfiles call will auto-load the following annotation pkg, but we preemptively & explicitly do it
   # here in order to control output of messages to stderr.
   basic.annPkgName <- cleanPlatformName(arrayTypeName)
   loadAnnotationPackage(basic.annPkgName)

   # Now, read the CEL files to be processed.
   tryCatch(
      {
         cel.batch <- read.celfiles(files.to.process, verbose=FALSE)
      },
      warning=function(w) {
         # Check whether read.celfiles detected any duplicated names.  This should not happen after our above check
         # for duplicates, but we'll do it just in case.
         if (grepl("non-unique values when setting 'row.names'", conditionMessage(w))) {
            stop(paste0("Duplicated CEL file names were detected.  The module cannot handle duplicated names.  ",
                        "Each file must have a unique name (ignoring any compression extension)."))
         }
         else {
            print(conditionMessage(w))
         }
      }
   )

   # Rename samples according to CLM file, if present, or remove the '.CEL' extensions from the file names if not.
   column.names <- rename.samples(sampleNames(cel.batch), clm)

   # The following line is the key call to extract and preprocess the expression values from the CELS
   coreTranscript.summary <- rma(cel.batch, target="core", background=background.correct, normalize=normalize)

   expr.data <- exprs(coreTranscript.summary)
   
   if (annotate.probes) {
      # Check arrayTypeName to see if we have Human, Mouse, Rat (using hard-coded huex, hugene, ...)
      # These arrays have *much* better/cleaner annotation info available in secondary "transcriptcluster" packages;
      # it's much better to work with those than the corresponding pdInfoFile where they are available.
      if (haveDetailedAnnotations(arrayTypeName)) {
         # Remove any trailing "-v1", "-v2" piece (if present) as these are not found in the "transcriptcluster" package names.
         # This cleans up e.g. "HuEx-1_0-st-v2" to "HuEx-1_0-st".
         transcriptClusterArrayTypeName <- gsub("-v[12]$", "", arrayTypeName)
         # ...then remove dash and underscore chars and force to lowercase as expected in these names.
         transcriptClusterArrayTypeName <- tolower(gsub("[-_]", "", transcriptClusterArrayTypeName))
 
         transcriptClusterDbName <- paste0(transcriptClusterArrayTypeName, "transcriptcluster")
         transcriptClusterDb.annPkgName <- paste0(transcriptClusterDbName, ".db")
         loadAnnotationPackage(transcriptClusterDb.annPkgName)
         annotations <- build.annotations(coreTranscript.summary, transcriptClusterDbName)
      }
      else {
         # For other organisms, skip annotations entirely
         print(paste0("Sorry, annotation information is not available for arrays of type ", arrayTypeName, " at this time."))
         annotate.probes <- FALSE
         annotations <- NULL
      }
   }
   else {
      annotations <- NULL
   }

   dataset <- list(row.descriptions=annotations, data=expr.data)
   colnames(dataset$data) <- column.names
   print("Writing dataset as GCT")
   write.gct(dataset, output.file.base)

   plot.qc.images(cel.batch, column.names, output.file.base, qc.plot.format)
}

loadAnnotationPackage <- function(annPackageName) {
   # Dynamically install (if necessary) and load the extra required annotation package
   dyn.loadBioconductorPackage(annPackageName)
   suppressMessages(suppressWarnings(
      library(annPackageName, character.only=TRUE, warn.conflicts=FALSE)
   ))
}

# Works for Human, Mouse, Rat but not for other organisms.
build.annotations <- function(coreTranscript.summary, transcriptClusterDbName) {
   featureData(coreTranscript.summary) <- getNetAffx(coreTranscript.summary, "transcript")

   # Find references to the various annotation environments needed for lookup.  These are dependent on
   # the transcriptClusterDbName (based on the Affy array type).
   # That is, we will be looking in e.g. mogene20sttranscriptclusterENTREZID for the Entrez Gene identifiers.
   print("Finding annotation environment references")
   entrezMapName <- paste0(transcriptClusterDbName,"ENTREZID")
   entrezEnv <- get(entrezMapName)
   symbolMapName <- paste0(transcriptClusterDbName,"SYMBOL")
   symbolEnv <- get(symbolMapName)

   # Now we look up the various annotation info.  The annotatedData structure will have rownames matching
   # the Affy transcript Ids, so we apply a lookup function across these to get the Entrez Gene ID, the gene
   # description and the gene symbol.  These look into the environments found in the transcriptClusterDbName package
   # loaded above.  There's a little bit of 'meta-programming' here; see the mogene20transcriptcluster package
   # documentation in Bioconductor for a more 'straight code' version of how to do this.  The key point
   # here is that we find the relevant annotation records through the (matrix-oriented) 'mget' call, then
   # apply a function to pull out the desired piece of info from the record.
   print("Looking up annotations")
   annotatedData <- featureData(coreTranscript.summary)
   entrezGenes <- sapply(mget(rownames(annotatedData), entrezEnv), FUN=function(entrezEntry) (entrezEntry[[1]]), simplify="array")
   gene.symbols <- sapply(mget(rownames(annotatedData), symbolEnv), FUN=function(symbolEntry) (symbolEntry[[1]]), simplify="array")
      
   # The RefSeq Ids are available in the base annotation package loaded by oligo for this array type.  We
   # could get it from an environment like the items above, but it's a little more straightforward this way. 
   refSeqIds <- sapply(pData(annotatedData)[,"geneassignment"], FUN=function(geneEntry) (unlist(strsplit(geneEntry," //"))[1]), simplify="array")

   # Build a suitable annotation for the dataset.  We'll use "<entrezGene> // <refSeqId> // <gene.symbol>"
   print("Annotating dataset")
   annotations <- paste0(unname(entrezGenes), " // ", unname(refSeqIds), " // ", unname(gene.symbols))
   return (annotations)
}

getCelChipType <- function(x) {
   read.celfile.header(x)[["cdfName"]]
}

haveSTArrayType <- function(arrayTypeName) {
   return(grepl(".st$|.st.v1$|.st.v2$", arrayTypeName, ignore.case=TRUE))
}

haveDetailedAnnotations <- function(arrayTypeName) {
   # Bioconductor only has detailed annotation information for Human, Mouse, and Rat.
   return(grepl("huex|hugene|moex|mogene|raex|ragene", arrayTypeName, ignore.case=TRUE))
}

rearrange.files <- function(file.list, clm) {
   # Rearrange the order of the files to match the CLM file.  Note that we may discard
   # some files based on this (if they are not present in the CLM).
   print("Rearranging samples based on CLM file")
   new.files.list <- c()
   file.basenames <- basename(file.list)
   scanIdxs.to.remove <- c()
   scan.index <- 1
   new.index <- 1
   del.index <- 1
   for (scan in clm$scan.names) {
      # Search for the scan name in the file.list.  We search against the file.basenames only so that there
      # are only matches against file names rather than some other path component.
      scanPat <- paste0("^", scan, "$")  # We want an exact match
      index <- grep(scanPat, file.basenames, ignore.case=TRUE)

      if (length(index) == 0) {
         cat(paste("Scan", scan, "in clm file was not found. \n"))
         scanIdxs.to.remove[del.index] <- scan.index
         del.index <- del.index + 1
      } 
      else if(length(index) > 1) {
         cat(paste("Scan", scan, "in clm file matches more than one CEL file. \n"))
      } 
      else {
         # Pull the file out of the *original* list so that it has the full path info.
         new.files.list[new.index] <- file.list[index[1]]
         new.index <- new.index + 1
      }
      scan.index <- scan.index + 1
   }
   
   # Remove any unused scans from the CLM.
   if (length(scanIdxs.to.remove) > 0) {
      clm$scan.names <- clm$scan.names[-scanIdxs.to.remove]
      clm$factor <- clm$factor[-scanIdxs.to.remove]
      clm$sample.names <- clm$sample.names[-scanIdxs.to.remove]
   }
   
   return (list("file.list"=new.files.list, "clm"=clm))
}

# Based on code from ExpressionFileCreator.  Migrate this to a common lib?  Only common between these two for now.
rename.samples <- function(cel.file.names, clm) {
   # If there is no CLM, just strip the extensions and use the file names
   if(is.null(clm)) {
      new.cel.file.names <- gsub(".CEL$|.CEL.gz$|.CEL.zip$|.CEL.bz2$", "", cel.file.names, ignore.case=TRUE)
      return (new.cel.file.names)
   }
   
   print("Renaming samples based on CLM file")
   
   # Strip extensions from both the CLM scan.names and the cel.file.names to simplify matching
   scan.names <- gsub(".CEL$|.CEL.gz$|.CEL.zip$|.CEL.bz2$", "", clm$scan.names, ignore.case=TRUE)
   cel.file.names <- gsub(".CEL$|.CEL.gz$|.CEL.zip$|.CEL.bz2$", "", cel.file.names, ignore.case=TRUE)
   
   new.cel.file.names <- vector("character")
   i <- 1
   scanIdx <- 1
   remove.scan.index <- c()
   for (scan in scan.names) {
      s <- paste0('^', scan, "$")
      index <- grep(s, cel.file.names, ignore.case=TRUE)

      if (length(index) == 0) {
         cat(paste("Scan", scan, "in clm file was not found.  Excluding from dataset. \n"))
         remove.scan.index <- c(remove.scan.index, scanIdx)
      } 
      else if(length(index) > 1) {
         cat(paste("Scan", scan, "in clm file matches more than one CEL file.  Excluding from dataset. \n"))
         remove.scan.index <- c(remove.scan.index, scanIdx)
      } 
      else {
         new.cel.file.names[i] <- cel.file.names[index[1]]
         i <- i + 1
      }   
      scanIdx <- scanIdx + 1
   }
   cel.file.names <- new.cel.file.names

   # Remove duplicate or missing scan names from the CLM
   if (length(remove.scan.index) != 0) {
      clm$scan.names <- clm$scan.names[-remove.scan.index]
      clm$sample.names <- clm$sample.names[-remove.scan.index]
             
      if (!is.null(clm$factor)) {
         clm$factor <- clm$factor[-remove.scan.index, drop=TRUE]
      }
   }

   if (length(cel.file.names) == 0) {
      exit("No CEL files listed in clm file found.")
   }

   return (clm$sample.names)
}

write.cls.from.clm <- function(clm, output.file.base) {
   if (!is.null(clm)) {
      factor <- clm$factor
      if (!is.null(factor)) {
         output.cls.file.name <- paste0(output.file.base, ".cls")        
         write.factor.to.cls (factor, output.cls.file.name)
      }
   }
}

plot.qc.images <- function(cel.batch, column.names, output.file.base, qc.plot.format) {
   # Print out some QC images. Plot nothing if the user chose "skip"
   device.open <- get.device.open(qc.plot.format)
   if (!is.null(device.open)) {
      print("Generating QC plots")
      print.densityHistogram(cel.batch, device.open, output.file.base, NROW(column.names))
      print.boxplot(cel.batch, device.open, output.file.base, NROW(column.names))
      
      for (i in 1:NROW(column.names)) {
         print.MAplot(cel.batch, device.open, output.file.base, i, column.names[i])
         print.celImage(cel.batch, device.open, output.file.base, i, column.names[i])
      }
   }
}

check.output.format <- function(output.format) {
   if (!(output.format %in% c("pdf", "svg", "png", "skip"))) {
      stop(paste0("Unrecognized output format '", output.format, "'"))
   }
}

get.device.open <- function(extension) {
   if (extension == "skip") { return(NULL) }
   if (extension == "pdf") {
      return(function(filename_base) {
         pdf(paste0(filename_base, ".pdf"))
      })
   }
   if (extension == "svg") {
      return(function(filename_base) {
         svg(paste0(filename_base, ".svg"))
      })
   }
   if (extension == "png") {
      return(function(filename_base) {
         png(paste0(filename_base, ".png"))
      })
   }
   stop(paste0("Unhandled plot file format '", extension, "'"))
}

print.plotObject <- function(plotObj, filename_base, device.open) {
   device.open(filename_base)
   print(plotObj)
   dev.off()
}

build.allCelPlotter <- function(plotTypeName, plotterFunction) {
   function(cel.batch, device.open, output.file.base, count) {
      plotname <- paste0(output.file.base, ".QC.",plotTypeName)
      tryCatch({
         device.open(plotname)
         plotterFunction(cel.batch, count)
         dev.off()
      },
      error = function(err) {
         print(paste0("Error printing the ", plotname, " plot - skipping"))
         print(conditionMessage(err))
      })
   }
}

build.oneCelPlotter <- function(plotTypeName, plotterFunction) {
   function(cel.batch, device.open, output.file.base, which, plotInstanceName) {
      plotname <- paste0(output.file.base, ".QC.", plotInstanceName, "_", plotTypeName)
      tryCatch({
         device.open(plotname)
         plotterFunction(cel.batch, which)
         dev.off()
      },
      error = function(err) {
         print(paste0("Error printing the ", plotname, " plot - skipping"))
         print(conditionMessage(err))
      })
   }
}

print.densityHistogram <- build.allCelPlotter("Density_histogram", 
   function(cel.batch, count) {
      hist(cel.batch, names=1:count)
   }
)

print.boxplot <- build.allCelPlotter("Boxplot", 
   function(cel.batch, count) {
      boxplot(cel.batch, names=1:count)
   }
)

print.celImage <- build.oneCelPlotter("Cel_image", 
   function(cel.batch, which) {
      image(cel.batch, which=which)
   }
)

print.MAplot <- build.oneCelPlotter("MAplot", 
   function(cel.batch, which) {
      MAplot(cel.batch, which=which)
   }
)

# Set up all of the input files in a common directory for processing.
GP.setup.input.files <- function(input.file, destdir) {
   # Create a subdir to hold all of the input files.
   dir.create(destdir)
   file.list <- read.table(input.file, header=FALSE, stringsAsFactors=FALSE)[,1]

   # Find files with various extensions so they can be handled according to type.
   # We copy/unpack all files into dedicated subdirectories within the cel_files location.  This is to avoid silently
   # overwriting files provided by other means (brought in individually or contained in multiple archives).  The
   # read.celfiles call will detect and disallow any duplicates, but handling archives this way ensures that collisions
   # will be detected and seen by the user rather than silently skipped.
   tmpDirCount <- 0
   
   cels<-grep("*.CEL$|*.CEL.gz$", ignore.case=TRUE, file.list, value=TRUE)
   cels.bz2<-grep("*.CEL.bz2$", ignore.case=TRUE, file.list, value=TRUE)
   tars<-grep("*.tar$", ignore.case=TRUE, file.list, value=TRUE)
   tar_gzs<-grep("*.tar.gz$", ignore.case=TRUE, file.list, value=TRUE)
   tar_bz2s<-grep("*.tar.bz2$", ignore.case=TRUE, file.list, value=TRUE)
   tar_xzs<-grep("*.tar.xz$", ignore.case=TRUE, file.list, value=TRUE)
   zips<-grep("*.zip$", ignore.case=TRUE, file.list, value=TRUE)

   # Copy the cels into place.  GZ files are handled natively by read.celfiles.
   if (NROW(cels) > 1) {
      for (i in 1:NROW(cels)) {
         tmpDirCount <<- tmpDirCount+1
         to <- file.path(destdir, paste0("in",tmpDirCount))
         dir.create(to)
         retVal <- file.copy(cels[i], to)
         if (!retVal) { stop(paste0("Unable to make a local copy of '", cels[i], "'")) }
      }
   }

   # Decompress BZ2 files.
   lapply(cels.bz2, function(cel.bz2) {
      tmpDirCount <<- tmpDirCount+1
      cel <- file.path(destdir, paste0("in",tmpDirCount), gsub("[.]bz2$", "", cel.bz2))
      bunzip2(cel.bz2, cel, overwrite=FALSE, remove=FALSE)
   })

   # Unpack TARs and ZIPs
   lapply(tars, function(tarfile) {
      tmpDirCount <<- tmpDirCount+1
      to <- file.path(destdir, paste0("in",tmpDirCount))
      untar(tarfile, exdir=to, tar="internal")
   })
   lapply(tar_gzs, function(tarfile) {
      tmpDirCount <<- tmpDirCount+1
      to <- file.path(destdir, paste0("in",tmpDirCount))
      untar(tarfile, exdir=to, compressed="gzip", tar="internal")
   })
   lapply(tar_bz2s, function(tarfile) {
      tmpDirCount <<- tmpDirCount+1
      to <- file.path(destdir, paste0("in",tmpDirCount))
      untar(tarfile, exdir=to, compressed="bzip2", tar="internal")
   })
   lapply(tar_xzs, function(tarfile) {
      tmpDirCount <<- tmpDirCount+1
      to <- file.path(destdir, paste0("in",tmpDirCount))
      untar(tarfile, exdir=to, compressed="xz", tar="internal")
   })
   lapply(zips, function(zipfile) {
      tmpDirCount <<- tmpDirCount+1
      to <- file.path(destdir, paste0("in",tmpDirCount))
      unzip(zipfile, exdir=to)
   })

   # Find all directory inputs.  These are handled differently: they will be passed in directly rather than being copied over.
   # We'll have the list.celfiles() call operate across these so the results will all be appended together for processing.
   input.file.info <- file.info(file.list)
   file.dirs.list <- rownames(subset(input.file.info, isdir==TRUE))
   
   # Add the destdir to our list of directories to process. 
   file.dirs.list <- c(destdir, file.dirs.list)

   # Gather up a list of all CEL files found in these dirs and return it for processing. 
   files.to.process <- list.celfiles(file.dirs.list, recursive=TRUE, full.names=TRUE, listGzipped=TRUE)
   
   return(files.to.process)   
}

check.for.dup.file <- function(files.to.process) {
   # Make sure that there are no name collisions (CEL files with duplicated names)
   # 1) Strip any compression extensions
   cel_names <- gsub("[.]gz", "", ignore.case=TRUE, basename(files.to.process))
   # 2) Force all CEL file extensions to same case
   cel_names <- gsub("[.]cel", ".CEL", ignore.case=TRUE, cel_names)
   dups <- duplicated(cel_names)
   if (any(dups)) {
      cat("The following CEL file names were duplicated  (ignoring any compression extension):\n", file=stderr())
      for (i in 1:NROW(dups)) {
         if (dups[i]) { cat(paste0("     ", basename(cel_names[i]), "\n"), file=stderr()) }
      }
      
      stop("The module cannot handle duplicated names.  Each file must have a unique name.")
   }
}
