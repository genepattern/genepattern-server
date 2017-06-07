## The Broad Institute
## SOFTWARE COPYRIGHT NOTICE AGREEMENT
## This software and its documentation are copyright (2013) by the
## Broad Institute/Massachusetts Institute of Technology. All rights are
## reserved.
##
## This software is supplied without any warranty or guaranteed support
## whatsoever. Neither the Broad Institute nor MIT can be responsible for its
## use, misuse, or functionality.

suppressMessages(suppressWarnings(library(getopt)))
suppressMessages(suppressWarnings(library(optparse)))

sessionInfo()

args <- commandArgs(trailingOnly=TRUE)

libdir <- args[1]

option_list <- list(
  make_option("--input.file", dest="input.file"),
  make_option("--output.file.name", dest="output.file.name"),
  make_option("--scale.to.value", dest="scale.to.value", default=NULL),
  make_option("--threshold", dest="threshold", default=NULL),
  make_option("--ceiling", dest="ceiling", default=NULL),
  make_option("--shift", dest="shift", default=NULL)
  )

opt <- parse_args(OptionParser(option_list=option_list), positional_arguments=TRUE, args=args)
print(opt)
opts <- opt$options

# returns string w/o leading or trailing whitespace
trim <- function (x) gsub("^\\s+|\\s+$", "", x)

# Returns NULL if the value is blank or a numeric if the value parses as numeric.
# Otherwise it stops execution with an error.
require.numeric.or.null <- function(value, paramName) {
  if (is.null(value)) { return(NULL) }
  trimmedValue = trim(value)
  if (trimmedValue == "") { return(NULL) }
  suppressWarnings(val <- as.numeric(trimmedValue))
  if (is.na(val)) {
      stop(paste("The parameter '", paramName, "' must be numeric or blank.  Received '", value, "'.", sep=""))  
  }
  return(val)
}

scale <- require.numeric.or.null(opts$scale.to.value, "scale.to.value")
threshold <- require.numeric.or.null(opts$threshold, "threshold")
ceiling <- require.numeric.or.null(opts$ceiling, "ceiling")
shift <- require.numeric.or.null(opts$shift, "shift")

source(file.path(libdir, "common.R"))
source(file.path(libdir, "rank_normalize.R"))

Rank.Normalize.Dataset(opts$input.file, opts$output.file.name, scale = scale, 
                       threshold = threshold, ceiling = ceiling, shift = shift)

sessionInfo()