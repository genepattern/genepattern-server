\name{run.analysis}
\alias{run.analysis}
\title{Runs an analysis module on a GenePattern server}

\description
{
	Accepts a module name or lsid to invoke, invokes the analysis
  	module on the GenePattern server and returns a JobResult object which contains the names of the
  	output files returned from the server. They can be retrieved later using \code{\link{job.result.get.output.filenames(job.result)}}.
}

\usage
{
	filenames <- run.analysis(gp.server, module.name.or.lsid, ...)
}

\arguments
{
	\item{gp.server}
	{
 		The URL (i.e.: "http://serverName:portNumber") of GenePattern server on which to run the module.
  	}
  	\item{module.name.or.lsid}
	{
  		The name or lsid of the module to execute on the GenePattern server.  The module must be accessible to the
		current user -- either as a public module or if private must be owned by the current user.
  	}
  	\item{...}
	{
  		All of the arguments required for the the named module to execute on the named server.  Module-optional arguments
		do need not to be specified.  Arguments set to NULL will be treated as missing (not sent to the server), so they are
		distinct from those set to "".  Non-optional arguments that are not provided will result in an error response
		from the GenePattern server when the module execution is attempted, which will be returned as a file named "stderr"
		in the results filename list.
	}
}

\details
{
	run.analysis performs the following functions:
  	\tabular{rl}
	{
		[,1] \tab submit the job to the GenePattern server\cr
		[,2] \tab wait for the server to execute the module and get the completed job\cr
		[,3] \tab return a JobResult object containing the list of filename results
  	}
}

\value{Returns a JobResult object which contains information about the analysis job that was executed.}

\author{Marc-Danie Nazaire, Joshua Gould, Broad Institute}

\examples
{
	\dontrun
	{
      	library(GenePattern)
		username <- "your email address"
		password <- "your password"
		servername <- "http://localhost:8080"
      	## Obtain a GPServer object
		gp.server <- gp.login(servername, username, password)
		input.ds <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
      	## run the TransposeDataset analysis on the GenePattern server
   		jobresults <- run.analysis(gp.server, "TransposeDataset", input.filename=input.ds)
  	}
}

\references
{
	GenePattern home: \url{http://www.broad.mit.edu/cancer/software/genepattern/}
	GenePattern help: \email{gp-help@broad.mit.edu}
}

\keyword{genepattern}
\keyword{programming}