#------------------------------------------------------------------------------------------------------
.onLoad <- function (libname, pkgname) {
	require ("rJava")
	fullPathToJars = paste (libname, pkgname, 'jars', 'GenePattern.jar', sep=.Platform$file.sep)
	.jinit (fullPathToJars)
	#Check that the java version is 1.5. or higher
	jvmVersion = .jcall ("java/lang/System", "S", "getProperty", "java.version")
    if (is.na (pmatch ("1.5.", jvmVersion))) {
		cat ('\n  You are using the wrong version of Java. Java version 1.5 is required. \n')
	}
}
#------------------------------------------------------------------------------------------------------
gp.login <- 
#
# Connect to gpServer.
#
function(server.name, user.name, password = NULL) 
{
    gp.server <- .jnew("org/genepattern/client/GPServer", server.name,  user.name, password)

    return(gp.server)
}	
#------------------------------------------------------------------------------------------------------
run.analysis <-
#
# Submits the given task with the given parameters and waits for the job to
# complete and returns a list of output files or NULL.
#
function(gp.server, module.name.or.lsid, ...)
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
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
		
	job.result <- .jcall(gp.server, "Lorg/genepattern/webservice/JobResult;", "runAnalysis", module.name.or.lsid, parameter.array)

	return(job.result)
}    	
#------------------------------------------------------------------------------------------------------
run.analysis.no.wait <-
#
# Submits the given module with the given parameters and does not wait for
# the job to complete.
#
function(gp.server, module.name.or.lsid, ...)
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
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

	result <- .jcall(gp.server, "I", "runAnalysisNoWait", module.name.or.lsid, parameter.array)

	return(result)
}
#------------------------------------------------------------------------------------------------------
create.job.result <-
function(gp.server, job.number)
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
	}

	job.number <- as.integer(job.number)

	if(!is.complete(gp.server, job.number))
		stop(paste("Cannot create JobResult: Job ", job.number, " has not completed"))
		
	job.result <- .jcall(gp.server, "Lorg/genepattern/webservice/JobResult;", "createJobResult", job.number)

	return (job.result)	
}
#------------------------------------------------------------------------------------------------------

is.complete <- 
#
# Checks if the given job is complete.
#
function(gp.server, job.number)
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
	}			

	job.number <- as.integer(job.number)

	complete <- .jcall(gp.server, "Z", "isComplete", job.number)

	return(complete)
}
#------------------------------------------------------------------------------------------------------
run.visualizer <-
#
# Submits the given visualizer with the given parameters and does not wait for
# the job to complete.
#
function(gp.server, module.name.or.lsid, ...)
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
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

	.jcall(gp.server,"V", "runVisualizer", module.name.or.lsid, parameter.array)
}
#------------------------------------------------------------------------------------------------------
get.parameters <-
#
# Returns a list of parameters for the specified module name or lsid.
# 
function(gp.server, module.name.or.lsid)
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
	}	

	parameter.info.array <- .jcall(gp.server,"[Lorg/genepattern/webservice/ParameterInfo;", "getParameters", module.name.or.lsid)
	
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
function(gp.server, module.name.or.lsid, filename) 
{
	if(is.null(gp.server))
	{
		stop("gp.server is NULL")
	}	

	url <- .jcall(gp.server,"Ljava/net/URL;", "getModuleFileUrl", module.name.or.lsid, filename)

	return(url)	
}


#*******************************************************************************************************
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
	
	url <- .jcall(job.result, "Ljava/net/URL;", "getURL", filename.or.file.output.order)

	return(url)
}
#------------------------------------------------------------------------------------------------------
job.result.get.output.filenames <-
function(job.result)
{
	output.filenames.array <- .jcall(job.result, "[S", "getOutputFileNames")

	output.filenames.list <- as.list(output.filenames.array)
	
	return(output.filenames.list)		
}
#------------------------------------------------------------------------------------------------------
job.result.get.url.for.file.type <-
function(job.result, file.type)
{
	if(is.null(job.result))
	{
		stop("job.result is NULL")
	}

	url <- .jcall(job.result, "Ljava/net/URL;", "getURLForFileType", file.type)

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

	job.number <- .jcall(job.result, "I", "getJobNumber")

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

	file <- .jcall(job.result, "Ljava/io/File;", "downloadFile", filename, download.directory)
	
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

	files.array <- .jcall(job.result, "[Ljava/io/File;", "downloadFiles", download.directory, overwrite)

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

	server.url <- .jcall(job.result, "Ljava/net/URL;", "getServerURL")

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
	
	lsid <- .jcall(job.result, "S", "getLSID")

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

	has.standard.out <- .jcall(job.result, "Z", "hasStandardOut")

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

	has.standard.error <- .jcall(job.result, "Z", "hasStandardError")

	return(has.standard.error) 	
}
