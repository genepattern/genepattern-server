\name{run.analysis.no.wait}
\alias{run.analysis.no.wait}
\title{Runs an analysis module on a GenePattern server}

\description
{
    Accepts a module name or lsid to invoke, invokes the analysis
    module asynchronously on the GenePattern server and returns the job number. The job number can later be used to retrieve a JobResult object using \code{\link{create.job.result(job.number)}}.
}

\usage{filenames <- run.analysis.no.wait(gp.server, module.name.or.lsid, ...)}

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
  	    All of the arguments required for the the named module to execute on the named server. Module-optional arguments
	    do need not to be specified. Arguments set to NULL will be treated as missing (not sent to the server), so they are
	    distinct from those set to "". Non-optional arguments that are not provided will result in an error response
	    from the GenePattern server when the module execution is attempted.
    }
}

\details
{
    run.analysis.no.wait performs the following functions:
    \tabular{rl}
    {
	    [,1] \tab submit the job to the GenePattern server without waiting for the job to complete\cr
	    [,2] \tab return the number of the job\cr
    }
}

\value
{
    Returns a the job number which can be used to obtain a JobResult object, that contains information about the analysis job that was executed.
}

\author{Marc-Danie Nazaire, Joshua Gould, Broad Institute}

\examples
{
    \dontrun
    {
        library(GenePattern)
	    username <- "your email address"
	    password <- "your password"
	    servername <- "http://localhost:8080"
        ## obtain a GPServer object
	    gp.server <- gp.login(servername, username, password)
	    input.ds <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
        ## run the TransposeDataset analysis on the GenePattern server
   	    job.number <- run.analysis.no.wait(gp.server, "TransposeDataset", input.filename=input.ds)
    }
}

\references
{
	GenePattern home: \url{http://www.broad.mit.edu/cancer/software/genepattern/}
	GenePattern help: \email{gp-help@broad.mit.edu}
}

\keyword{genepattern, programming}
