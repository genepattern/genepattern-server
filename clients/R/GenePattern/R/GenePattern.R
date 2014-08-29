# The Broad Institute
# SOFTWARE COPYRIGHT NOTICE AGREEMENT
# This software and its documentation are copyright (2003-2007) by the
# Broad Institute/Massachusetts Institute of Technology. All rights are
# reserved.

# This software is supplied without any warranty or guaranteed support
# whatsoever. Neither the Broad Institute nor MIT can be responsible for its
# use, misuse, or functionality.

#------------------------------------------------------------------------------------------------------
.onLoad <- function (libname, pkgname) {
	require ("rJava")
	fullPathToJars = paste (libname, pkgname, 'jars', 'GenePattern.jar', sep=.Platform$file.sep)
	.jinit (fullPathToJars)
	#Check that the java version is 1.7. or higher
	jvmVersion = .jcall ("java/lang/System", "S", "getProperty", "java.version")
	version <- strsplit(jvmVersion, "\\.")[[1]]
	major.version = version[1]
    if (major.version < 2) {
		minor.version = version[2]
		if(minor.version < 7) {
			stop ('You are using the wrong version of Java. Java version 1.7 or higher is required.\n')
		}
	}
}
#------------------------------------------------------------------------------------------------------
gp.login <-
#
# Connect to GenePattern server
#
function(server.name, user.name, password = NULL)
{
    gp.client <- .jnew("org/genepattern/client/GPClient", server.name,  user.name, password, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
    	e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

    return(gp.client)
}
#------------------------------------------------------------------------------------------------------
run.analysis <-
#
# Submits the given module with the given parameters and waits for the job to
# complete and returns a list of output files or NULL.
#
function(gp.client, module.name.or.lsid, ...)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	parameters <- list()
	index <- 1
	args <- list(...)
	for(name in names(args))
	{
		parameter <- .jnew("org/genepattern/webservice/Parameter", name, args[[name]])
		parameters[index] <- list(parameter)
		index <- index + 1
	}

	parameter.array <- .jarray(parameters, contents.class="org/genepattern/webservice/Parameter")

	job.result <- .jcall(gp.client, "Lorg/genepattern/webservice/JobResult;", "runAnalysis", module.name.or.lsid, parameter.array, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(job.result)
}
#------------------------------------------------------------------------------------------------------
run.analysis.no.wait <-
#
# Submits the given module with the given parameters and does not wait for
# the job to complete.
#
function(gp.client, module.name.or.lsid, ...)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	parameters <- list()
	index <- 1
	args <- list(...)
	for(name in names(args))
	{
		parameter <- .jnew("org/genepattern/webservice/Parameter", name, args[[name]])
		parameters[index] <- list(parameter)
		index <- index + 1
	}

	parameter.array <- .jarray(parameters, contents.class="org/genepattern/webservice/Parameter")

	result <- .jcall(gp.client, "I", "runAnalysisNoWait", module.name.or.lsid, parameter.array, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(result)
}
#------------------------------------------------------------------------------------------------------
create.job.result <-
function(gp.client, job.number)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	job.number <- as.integer(job.number)

	if(!is.complete(gp.client, job.number))
		stop(paste("Cannot create JobResult: Job ", job.number, " has not completed"))

	job.result <- .jcall(gp.client, "Lorg/genepattern/webservice/JobResult;", "createJobResult", job.number, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return (job.result)
}
#------------------------------------------------------------------------------------------------------
is.complete <-
#
# Checks if the given job is complete.
#
function(gp.client, job.number)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	job.number <- as.integer(job.number)

	complete <- .jcall(gp.client, "Z", "isComplete", job.number, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(complete)
}
#------------------------------------------------------------------------------------------------------
run.visualizer <-
#
# Submits the given visualizer with the given parameters and does not wait for
# the job to complete.
#
function(gp.client, module.name.or.lsid, ...)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	parameters <- list()
	index <- 1
	args <- list(...)
	for(name in names(args))
	{
		parameter <- .jnew("org/genepattern/webservice/Parameter", name, args[[name]])
		parameters[index] <- list(parameter)
		index <- index + 1
	}

	parameter.array <- .jarray(parameters, contents.class="org/genepattern/webservice/Parameter")

	.jcall(gp.client,"V", "runVisualizer", module.name.or.lsid, parameter.array, check = FALSE)

	if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }
}
#------------------------------------------------------------------------------------------------------
get.parameters <-
#
# Returns a list of parameters for the specified module name or lsid.
#
function(gp.client, module.name.or.lsid)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	parameter.info.array <- .jcall(gp.client,"[Lorg/genepattern/webservice/ParameterInfo;", "getParameters", module.name.or.lsid, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	if(is.null(parameter.info.array))
		return(NULL)

	parameter.info.list <- list()
	for(i in 1:length(parameter.info.array))
	{
		parameter.info <- parameter.info.array[[i]]

		parameter.info.list[i] <- .jcall(parameter.info, "S", "toString")
	}

	return(parameter.info.list)
}
#------------------------------------------------------------------------------------------------------
is.windows <-
function()
{
	Sys.info()[["sysname"]]=="Windows"
}
#------------------------------------------------------------------------------------------------------
input.prompt <-
function(prompt)
{
	if (is.windows())
	{
		return(winDialogString(message = prompt, default = ""))
	}
	else
	{
		cat(prompt)
		ret <- readLines(stdin(), n=1, ok=TRUE)[[1]];
		return(ret)
	}
}
#------------------------------------------------------------------------------------------------------
gp.get.module.file.url <-
#
#Returns the url to retrieve the given file as part of the given module.
#
function(gp.client, module.name.or.lsid, filename)
{
	if(is.null(gp.client))
	{
		stop("gp.client is NULL")
	}

	url <- .jcall(gp.client,"Ljava/net/URL;", "getModuleFileUrl", module.name.or.lsid, filename, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

    url <- .jcall(url, "S", "toString")

	return(url)
}
#------------------------------------------------------------------------------------------------------
job.result.get.url <-
#
#Returns the url to download the given file name.
#
function(job.result, filename.or.file.output.order)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	if(is.numeric(filename.or.file.output.order))
		filename.or.file.output.order <- as.integer(filename.or.file.output.order)

	url <- .jcall(job.result, "Ljava/net/URL;", "getURL", filename.or.file.output.order, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	url <- .jcall(url, "S", "toString")

	return(url)
}
#------------------------------------------------------------------------------------------------------
job.result.get.output.filenames <-
function(job.result)
{
	output.filenames.array <- .jcall(job.result, "[S", "getOutputFileNames", check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	output.filenames.list <- as.list(output.filenames.array)

	return(output.filenames.list)
}
#------------------------------------------------------------------------------------------------------
job.result.get.url.for.file.name <-
function(job.result, file.name)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	url <- .jcall(job.result, "Ljava/net/URL;", "getURLForFileName", file.name, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	url <- .jcall(url, "S", "toString")

	return(url)
}
#------------------------------------------------------------------------------------------------------
job.result.get.job.number <-
function(job.result)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	job.number <- .jcall(job.result, "I", "getJobNumber", check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(job.number)
}
#------------------------------------------------------------------------------------------------------
job.result.download.file <-
function(job.result, filename, download.directory, overwrite = TRUE)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	if(is.null(download.directory))
	{
		stop("download directory cannot be NULL")
	}

	file <- .jcall(job.result, "Ljava/io/File;", "downloadFile", filename, download.directory, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

    if(!is.null(file))
	    filename <- .jcall(file, "S", "getAbsolutePath")

	return(filename)
}
#------------------------------------------------------------------------------------------------------
job.result.download.files <-
function(job.result, download.directory, overwrite = TRUE)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	if(is.null(download.directory))
	{
		stop("download directory cannot be NULL")
	}

	files.array <- .jcall(job.result, "[Ljava/io/File;", "downloadFiles", download.directory, overwrite, check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	filenames.list <- list()
	for(i in 1:length(files.array))
	{
		file <- files.array[[i]]
		filenames.list[i] <- .jcall(file, "S", "getAbsolutePath")
	}

	return(filenames.list)
}
#------------------------------------------------------------------------------------------------------
job.result.get.server.url <-
function(job.result)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")

	}

	server.url <- .jcall(job.result, "Ljava/net/URL;", "getServerURL", check = FALSE)

	if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	server.url <- .jcall(server.url, "S", "toString")

	return(server.url)
}
#------------------------------------------------------------------------------------------------------
job.result.get.lsid <-
function(job.result)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	lsid <- .jcall(job.result, "S", "getLSID", check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(lsid)
}
#------------------------------------------------------------------------------------------------------
job.result.has.standard.out <-
function(job.result)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	has.standard.out <- .jcall(job.result, "Z", "hasStandardOut", check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(has.standard.out)
}
#------------------------------------------------------------------------------------------------------
job.result.has.standard.error <-
function(job.result)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	has.standard.error <- .jcall(job.result, "Z", "hasStandardError", check = FALSE)

    if (!is.null(e <-.jgetEx(clear = TRUE)))
    {
        e.message <- .jcall(e, "S", "toString")
    	stop(e.message)
    }

	return(has.standard.error)
}
#------------------------------------------------------------------------------------------------------


# extension e.g. '.gct'
check.extension <- function(file.name, extension) {
	ext <- regexpr(paste(extension,"$",sep=""), tolower(file.name))
	if(ext[[1]] == -1) {
		file.name <- paste(file.name, extension, sep="")
	}
	return(file.name)
}
#------------------------------------------------------------------------------------------------------
read.dataset <- function(file) {
	result <- regexpr(paste(".gct","$",sep=""), tolower(file))
	if(result[[1]] != -1)
		return(read.gct(file))
	result <- regexpr(paste(".res","$",sep=""), tolower(file))
	if(result[[1]] != -1)
		return(read.res(file))

	stop("Input is not a res or gct file.")
}
#------------------------------------------------------------------------------------------------------
read.gct <- function(file) {
	if (is.character(file))
        if (file == "")
            file <- stdin()
        else {
            file <- file(file, "r")
            on.exit(close(file))
        }
	if (!inherits(file, "connection"))
        stop("argument `file' must be a character string or connection")

   # line 1 version
	version <- readLines(file, n=1)

	# line 2 dimensions
	dimensions <- scan(file, what=list("integer", "integer"), nmax=1, quiet=TRUE)
	rows <- dimensions[[1]]
	columns <- dimensions[[2]]
	# line 3 Name\tDescription\tSample names...
	column.names <- read.table(file, header=FALSE, nrows=1, sep="\t", fill=FALSE)
	column.names <-column.names[3:length(column.names)]


	if(length(column.names)!=columns) {
		stop(paste("Number of sample names", length(column.names), "not equal to the number of columns", columns, "."))
	}

	colClasses <- c(rep(c("character"), 2), rep(c("double"), columns))

	x <- read.table(file, header=FALSE, quote="", row.names=NULL, comment.char="", sep="\t", colClasses=colClasses, fill=FALSE)
	row.descriptions <- as.character(x[,2])
	data <- as.matrix(x[seq(from=3, to=dim(x)[2], by=1)])

	column.names <- column.names[!is.na(column.names)]

	colnames(data) <- column.names
	row.names(data) <- x[,1]
	return(list(row.descriptions=row.descriptions, data=data))
}
#------------------------------------------------------------------------------------------------------
read.res <- function(filename)
{
  # read line 1: sample names
  headings <- read.table( filename, header=FALSE, nrows=1, sep="\t", fill=FALSE)
  # delete the NA entries for the tab-tab columns
  headings <- headings[!is.na(headings)]
  colNames <- headings[3:length(headings)]

  # read line 2: sample descriptions
  descriptions <- scan(filename, skip=1, nlines=1, sep="\t", fill=F, blank.lines.skip=F, quiet=T, what="character")
  # delete the NA entries for the tab-tab columns
  descriptions <- descriptions[!is.na(descriptions)]
  if(length(descriptions) > 0) {
  	descriptions <- descriptions[3:length(descriptions)]
  }
  # handle optionally missing number of lines (not used, but need to decide whether to ignore before actual data)
  numLines <- as.list(read.table(filename, header=FALSE, skip=2, nrows=1, sep="\t", fill=FALSE))
  numLines <- numLines[!is.na(numLines)] # remove NA entries
  skip <- (3 - ifelse(length(numLines) == 1, 0, 1)) # skip 3 lines if line number is present, 2 otherwise

  columns <- length(headings) - 2 # substract 2 for gene description and name
  colClasses <- c(c("character", "character"), rep(c("double", "character"), columns))


  x <- .my.read.table(filename, header=FALSE, sep="\t", comment.char="", skip=skip, colClasses=colClasses, row.names=NULL, quote=NULL, fill=FALSE)

  data <- as.matrix(x[c(seq(from=3,length=(dim(x)[2]-3)/2, by=2))])
  calls <- as.matrix(x[c(seq(from=4,length=(dim(x)[2]-3)/2, by=2))])

  row.names <- x[,2]
  row.names(data) <- row.names
  row.names(calls) <- row.names
  row.descriptions <- as.character(x[, 1])
  colnames(data) <- colNames
  colnames(calls) <- colNames
  return(list(row.descriptions=row.descriptions, column.descriptions=descriptions, data=data, calls=calls))
}
#------------------------------------------------------------------------------------------------------
write.res <-
#
# write a res structure as a file
#
function(res, filename, check.file.extension=TRUE)
{
	if(check.file.extension) {
		filename <- check.extension(filename, ".res")
	}
	f <- file(filename, "w")
	on.exit(close(f))
	# write the labels
	cat("Description\tAccession\t", file=f, append=TRUE)
	cat(colnames(res$data), sep="\t\t", file=f, append=TRUE)
	cat("\n", file=f, append=TRUE)

	# write the descriptions
	if(!is.null(res$column.descriptions)) {
		cat("\t", file=f, append=TRUE)
		cat(res$column.descriptions, sep="\t\t", file=f, append=TRUE)
	}
	cat("\n", file=f, append=TRUE)

	# write the size
	cat(NROW(res$data), "\n", sep="", file=f, append=TRUE)

	# write the data
	# 1st combine matrices
	dim <- dim(res$data)
	dim[2] <- dim[2]*2

	m <- matrix(nrow=dim[1], ncol=dim[2]+2)

	if(!is.null(res$row.descriptions)) {
		m[,1] <- res$row.descriptions
	} else {
		m[, 1] <- ''
	}

	m[,2] <- row.names(res$data)

	index <- 3
	for(i in 1:dim(res$data)[2]) {
		m[,index] <- res$data[,i]
		index <- index + 2
	}
	index <- 4

	for(i in 1:dim(res$calls)[2]) {
		m[,index] <- as.character(res$calls[,i])
		index <- index + 2
	}
	write.table(m, file=f, col.names=FALSE, row.names=FALSE, append=TRUE, quote=FALSE, sep="\t", eol="\n")
	return(filename)
}
#------------------------------------------------------------------------------------------------------
read.cls <- function(file) {
	# returns a list containing the following components:
	# labels the factor of class labels
	# names the names of the class labels if present

	if (is.character(file))
        if (file == "")
            file <- stdin()
        else {
            file <- file(file, "r")
            on.exit(close(file))
        }
    if (!inherits(file, "connection"))
        stop("argument `file' must be a character string or connection")

	line1 <- scan(file, nlines=1, what="character", quiet=TRUE)

	numberOfDataPoints <- as.integer(line1[1])
	numberOfClasses <- as.integer(line1[2])

	line2 <- scan(file, nlines=1, what="character", quiet=TRUE)

	classNames <- NULL
	if(line2[1] =='#') { # class names are given
		classNames <- as.vector(line2[2:length(line2)])
		line3 <- scan(file, what="character", nlines=1, quiet=TRUE)
	} else {
		line3 <- line2
	}

	if(is.null(classNames)) {
		labels <- as.factor(line3)
		classNames <- levels(labels)
	} else {
		labels <- factor(line3, labels=classNames)
	}
	if(numberOfDataPoints!=length(labels)) {
		stop("Incorrect number of data points")
	}
	r <- list(labels=labels,names=classNames)
	r
}
#------------------------------------------------------------------------------------------------------
write.cls <-
#
# writes a cls result to a file. A cls results is a list containing names and labels
function(cls, filename, check.file.extension=TRUE)
{
	if(check.file.extension) {
		filename <- check.extension(filename, ".cls")
	}
	file <- file(filename, "w")
	on.exit(close(file))

	cat(file=file, length(cls$labels), length(levels(cls$labels)), "1\n")

    # write cls names
	if(length(cls$names) > 0) {
		cat(file=file, "# ")
        i <- 1
		while(i < length(cls$names)) {
			cat(file=file, cls$names[i])
			cat(file=file, " ")

			i <- i+1
		}
		cat(file=file, cls$names[length(cls$names)])
		cat(file=file, "\n")
	}

   # write cls labels
	i <-1
	while(i < length(cls$labels)){
		cat(file=file, as.numeric(cls$labels[[i]])-1)
		cat(file=file, " ")

		i <- i+1
	}
	cat(file=file, as.numeric(cls$labels[[length(cls$labels)]])-1)

	return(filename)
}
#------------------------------------------------------------------------------------------------------
write.gct <-
#
# save a GCT result to a file, ensuring the filename has the extension .gct
#
function(gct, filename, check.file.extension=TRUE)
{
	if(check.file.extension) {
		filename <- check.extension(filename, ".gct")
	}
	f <- file(filename, "w")
	on.exit(close(f))


	cat("#1.2", "\n", file=f, append=TRUE, sep="")
	cat(dim(gct$data)[1], "\t", dim(gct$data)[2], "\n", file=f, append=TRUE, sep="")
	cat("Name", "\t", file=f, append=TRUE, sep="")
	cat("Description", file=f, append=TRUE, sep="")
	names <- colnames(gct$data)

	for(j in 1:length(names)) {
		cat("\t", names[j], file=f, append=TRUE, sep="")
	}

	cat("\n", file=f, append=TRUE, sep="")
	m <- matrix(nrow = dim(gct$data)[1], ncol = 2)

	m[, 1] <- row.names(gct$data)

	if(!is.null(gct$row.descriptions)) {
		m[, 2] <- gct$row.descriptions
	} else {
		m[, 2] <- ''
	}
	m <- cbind(m, gct$data)

	write.table(m, file=f, append=TRUE, quote=FALSE, sep="\t", eol="\n", col.names=FALSE, row.names=FALSE)
	return(filename)
}
#------------------------------------------------------------------------------------------------------
# like read.table, but doesn't check to make sure all rows have same number of columns
.my.read.table <- function (file, header = FALSE, sep = "", quote = "\"'", dec = ".", row.names, col.names, as.is = FALSE, na.strings = "NA", colClasses, nrows = -1, skip = 0, check.names = TRUE, fill = !blank.lines.skip, strip.white = FALSE, blank.lines.skip = TRUE, comment.char = "")
{
	if (is.character(file)) {
		file <- file(file, "r")
		on.exit(close(file))
	}
	if (!inherits(file, "connection"))
		stop("argument `file' must be a character string or connection")
	if (!isOpen(file)) {
		open(file, "r")
		on.exit(close(file))
	}
	if (skip > 0)
		readLines(file, skip)

	first <- readLines(file, n=1)
	pushBack(first, file)
	temp <- strsplit(first, "\t")
	cols <- as.integer(length(temp[[1]])) # number of columns

	if (missing(col.names))
        col.names <- paste("V", 1:cols, sep = "")

	what <- rep(list(""), cols)
	names(what) <- col.names
	colClasses[colClasses %in% c("real", "double")] <- "numeric"
	known <- colClasses %in% c("logical", "integer", "numeric", "complex", "character")
	what[known] <- sapply(colClasses[known], do.call, list(0))

	data <- scan(file = file, what = what, sep = sep, quote = quote, dec = dec, nmax = nrows, skip = 0, na.strings = na.strings, quiet = TRUE, fill = fill, strip.white = strip.white, blank.lines.skip = blank.lines.skip, multi.line = FALSE, comment.char = comment.char)
	nlines <- length(data[[1]])
	if (cols != length(data)) {
		warning(paste("cols =", cols, " != length(data) =", length(data)))
		cols <- length(data)
	}
	if (is.logical(as.is)) {
        as.is <- rep(as.is, length = cols)
	}
	else if (is.numeric(as.is)) {
	  if (any(as.is < 1 | as.is > cols))
			stop("invalid numeric as.is expression")
	  i <- rep(FALSE, cols)
	  i[as.is] <- TRUE
	  as.is <- i
	}
	else if (is.character(as.is)) {
	  i <- match(as.is, col.names, 0)
	  if (any(i <= 0))
			warning("not all columns named in as.is exist")
	  i <- i[i > 0]
	  as.is <- rep(FALSE, cols)
	  as.is[i] <- TRUE
	}
	else if (length(as.is) != cols)
		stop(paste("as.is has the wrong length", length(as.is),
			"!= cols =", cols))
	if (missing(row.names)) {
		if (rlabp) {
			row.names <- data[[1]]
			data <- data[-1]
	  }
	  else row.names <- as.character(seq(len = nlines))
	}
	else if (is.null(row.names)) {
		row.names <- as.character(seq(len = nlines))
	}
	else if (is.character(row.names)) {
		if (length(row.names) == 1) {
			rowvar <- (1:cols)[match(col.names, row.names, 0) ==
				 1]
			row.names <- data[[rowvar]]
			data <- data[-rowvar]
	  }
	}
	else if (is.numeric(row.names) && length(row.names) == 1) {
	  rlabp <- row.names
	  row.names <- data[[rlabp]]
	  data <- data[-rlabp]
	}
	else stop("invalid row.names specification")
	class(data) <- "data.frame"
	row.names(data) <- row.names
	data
}


