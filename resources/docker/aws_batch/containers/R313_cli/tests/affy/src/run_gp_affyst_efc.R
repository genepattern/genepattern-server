## The Broad Institute
## SOFTWARE COPYRIGHT NOTICE AGREEMENT
## This software and its documentation are copyright (2014) by the
## Broad Institute/Massachusetts Institute of Technology. All rights are
## reserved.
##
## This software is supplied without any warranty or guaranteed support
## whatsoever. Neither the Broad Institute nor MIT can be responsible for its
## use, misuse, or functionality.

suppressMessages(suppressWarnings(library(getopt)))
suppressMessages(suppressWarnings(library(optparse)))
suppressMessages(suppressWarnings(library(R.methodsS3)))
suppressMessages(suppressWarnings(library(R.oo)))
suppressMessages(suppressWarnings(library(R.utils)))
suppressMessages(suppressWarnings(library(DBI)))
suppressMessages(suppressWarnings(library(RSQLite)))
suppressMessages(suppressWarnings(library(parallel)))
suppressMessages(suppressWarnings(library(iterators)))
suppressMessages(suppressWarnings(library(foreach)))
suppressMessages(suppressWarnings(library(bit)))

# Don't load the 'ff' package even though it is specified in the r.package.info file.  It's needed
# to install the oligoClasses pkg but if it's loaded at runtime then it changes the behavior of
# the module, with failures as a result. 
#suppressMessages(suppressWarnings(library(ff)))

suppressMessages(suppressWarnings(library(BiocGenerics)))
suppressMessages(suppressWarnings(library(IRanges)))
suppressMessages(suppressWarnings(library(XVector)))
suppressMessages(suppressWarnings(library(GenomeInfoDb)))
suppressMessages(suppressWarnings(library(GenomicRanges)))
suppressMessages(suppressWarnings(library(Biobase)))
suppressMessages(suppressWarnings(library(zlibbioc)))
suppressMessages(suppressWarnings(library(Biostrings)))
suppressMessages(suppressWarnings(library(affyio)))
suppressMessages(suppressWarnings(library(affxparser)))
suppressMessages(suppressWarnings(library(preprocessCore)))
suppressMessages(suppressWarnings(library(oligoClasses)))
suppressMessages(suppressWarnings(library(oligo)))

sessionInfo()

args <- commandArgs(trailingOnly=TRUE)

libdir <- args[1]

option_list <- list(
  make_option("--input.file", dest="input.file"),
  make_option("--normalize", dest="normalize"),
  make_option("--background.correct", dest="background.correct"),
  make_option("--qc.plot.format", dest="qc.plot.format"),
  make_option("--clm.file", dest="clm.file", default=NULL),
  make_option("--annotate.rows", dest="annotate.rows"),
  make_option("--output.file.base", dest="output.file.base")
  )

opt <- parse_args(OptionParser(option_list=option_list), positional_arguments=TRUE, args=args)
print(opt)
opts <- opt$options

source(file.path(libdir, "common.R"))
source(file.path(libdir, "gp_affyst_efc.R"))

normalize <- (opts$normalize == "yes")
background.correct <- (opts$background.correct == "yes")
compute.present.absent.calls <- (opts$compute.present.absent.calls == "yes")

annotate.rows <- (opts$annotate.rows == "yes")

check.output.format(opts$qc.plot.format)

destdir <- "cel_files"
tryCatch(
   {
      files.to.process <- GP.setup.input.files(opts$input.file, destdir)
      GP.affyst.efc(files.to.process, normalize, background.correct, opts$qc.plot.format, 
                    opts$clm.file, annotate.rows, opts$output.file.base)
   },
   finally = {
      # Clean up the CEL file subtree.
      unlink(destdir, recursive=TRUE)
   }
)

sessionInfo()