#!/usr/bin/env Rscript
## Nicolas Stransky
## Cancer Program
## The Broad Institute
## stransky@broadinstitute.org

## R implementation of the python script for outlier removal.

## A point is considered an outlier if ALL of the following apply:
## 1) value != NA
## 2) abs (value - median_of_last_5_points) > 0.3 
## 3) abs (value - median_of_next_5_points) > 0.3 
## 4) abs (ln(value) - ln(median_of_last_5_points)) > 4
## 5) abs (ln(value) - ln(median_of_next_5_points)) > 4

## If within 5 of the end of the chromosome, pad the 5 points the median is
## taken of with copies of the value.

## If a point is an outlier, replace it with NA, or the median of 3 points - itself
## and its immediate neighbors, or the mean of its 2 neighbors.
## This replacement does not impact how other points are processed.  

#added by GP team in order to install R package plugin

args <- commandArgs(trailingOnly=TRUE)

vers <- "2.14"            # R version
libdir <- args[1]
server.dir <- args[2]
patch.dir <- args[3]
output.prefix <- args[4]

source(paste(libdir, "loadRLibrary.R", sep=''))
load.packages(libdir, patch.dir, server.dir, vers)

suppressPackageStartupMessages(require(Biobase))
suppressPackageStartupMessages(require(optparse))

option_list <-
  list(make_option(c("-v", "--verbose"), action="store_true", default=TRUE,
                   help="Print lots of output [default]"),
       make_option(c("-q", "--quietly"), action="store_false",
                   dest="verbose", help="Print little output"),
       make_option(c("-i", "--input"), action="store", type="character", 
                   help="Input file. Can be .txt .Rout .RData"),
       make_option(c("-o", "--outputdir"), action="store", type="character",
                   help="Output directory"),
       make_option(c("-m", "--mult_tol"), action="store", type="numeric", default=4,
                   help="Mutliplicative threshold [default %default]"),
       make_option(c("-a", "--add_tol"), action="store", type="numeric", default=0.3,
                   help="additive threshold [default %default]"),
       make_option(c("-n", "--trailingN"), action="store", type="numeric", default=5,
                   help="Number of trailing and leading probes to compute the median [default %default]"),
       make_option(c("-r", "--replacement"), action="store", type="character", default="NA",
                   help="Replacement value for the outlier probe. Valid choices are \"NA\", \"mean\" and \"median\" [default %default]")
       )

opt <- parse_args(OptionParser(option_list=option_list), positional_arguments = TRUE)

options <- opt$options
verbose <- options$verbose
mdQUADfile <- options$input
outputdir <- options$outputdir
mult_tol <- options$mult_tol
add_tol <- options$add_tol
trailingN <- options$trailingN
replacement <- options$replacement

catverbose <- function(string) {
  cat(format(Sys.time(), "%Y%m%d %H:%M:%S |"),string,"\n")
}

if (!file.exists(outputdir)) {
  stop(paste("Output directory does not exist:", outputdir))
}
if (!replacement %in% c("NA", "mean", "median")) {
  stop("Outlier replacement algorithm should be one of \"NA\" \"mean\" or \"median\"")
}
if (verbose) {
  catverbose("Loading data...")
}
if (grepl("\\.RData$|\\.Rout$",mdQUADfile)) {
  load(mdQUADfile)
  dat <- data.matrix(dat)
} else if (grepl("\\.txt$",mdQUADfile) || grepl("\\.cn$",mdQUADfile)) {
  dat <- data.matrix(read.table(mdQUADfile, header=TRUE, row.names=1, sep="\t", check.names=FALSE))
} else {
  stop("required format for input file is .cn|.txt|.Rout|.RData")
}

if(verbose) {
  catverbose("Data loaded. Processing chromosomes...")
}

k <- 0
newdat <- NULL

for (chromosome in unique(dat[, "Chromosome"])) {
  chrdat <- dat[dat[, "Chromosome"] == chromosome, ]
  tmpdat <- chrdat
  ## The first two colums are Chromosome and Position
  for (isample in 3:NCOL(chrdat)) {
    matrices <- vector("list", trailingN)
    medians <- vector("list", trailingN)
    first <- chrdat[1, isample]
    last <- chrdat[nrow(chrdat), isample]
    ## half <- (trailingN + 1L)%/%2L

    ## Build the matrices of values to compute the medians over a moving window, duplicate the first and last values as necessary
    for (i in seq_len(trailingN)) {
      matrices[[i]] <- matrix(
                              c(rep(first, (trailingN:1)[i]),
                                chrdat[,isample],
                                rep(last, 2*trailingN-((nrow(chrdat)+(trailingN:1)[i])%%trailingN))),
                              ncol=trailingN, byrow=TRUE)
    }
    ## Compute the medians
    medians <- lapply(matrices, function(x) rowMedians(x, na.rm=TRUE))
    
    ## Assemble all medians into a single matrix
    medians2 <- vector("numeric", length=sum(unlist(lapply(medians,length))))
    for (i in seq_len(trailingN)) {
      medians2[seq(from=i, by=trailingN, length=length(medians[[i]]))] <- medians[[i]]
    }

    ## List of outliers
    outliers <- (!is.na(chrdat[,isample]) &
                 abs(chrdat[,isample] - medians2[(1:NROW(chrdat))+trailingN+1]) > add_tol &
                 abs(chrdat[,isample] - medians2[1:NROW(chrdat)]) > add_tol &
                 abs(log(chrdat[,isample] / medians2[(1:NROW(chrdat))+trailingN+1])) > mult_tol &
                 abs(log(chrdat[,isample] / medians2[1:NROW(chrdat)])) > mult_tol)

    ## New value for the outliers:
    tmpdat[which(outliers), isample] <-
      switch(replacement,
             "NA"=NA,
             "mean"=rowMeans(cbind(
               chrdat[which(outliers)-1,isample],
               ## chrdat[which(outliers),isample],
               chrdat[which(outliers)+1,isample]), na.rm=TRUE),
             "median"=rowMedians(cbind(
               chrdat[which(outliers)-1,isample],
               chrdat[which(outliers),isample],
               chrdat[which(outliers)+1,isample]), na.rm=TRUE))
    
    ## if(verbose) progress((NCOL(chrdat)-2)*24,k <- k+1, k)
  }
  ## Chromosome by chromosme.
  newdat <- rbind(newdat, tmpdat)
  if(verbose) cat(chromosome,"")
}
if(verbose) cat("\n")

dat <- as.data.frame(newdat) ; rm(newdat)
dat[,1] <- as.integer(dat[,1])
dat[,2] <- as.integer(dat[,2])
filename <- paste(outputdir, paste(output.prefix,"no_outliers.cn", sep="."), sep="/")
if(verbose) catverbose("Write text...")
f <- file(filename, "w")
cat("Marker", file= f)
suppressWarnings(
                 ## Don't complain about appending columns to file
                 write.table(dat, file=f, sep="\t", quote=FALSE, row.names=TRUE,
                             col.names=NA, append=TRUE)
                 )
close(f)
if(verbose) catverbose("Write bin...")
save(dat, file=paste(outputdir, paste(output.prefix, "no_outliers.RData", sep="."), sep="/"))
if(verbose) catverbose("Done")
