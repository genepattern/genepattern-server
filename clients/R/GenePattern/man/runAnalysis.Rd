\name{runAnalysis}
\alias{runAnalysis}
\title{Runs an analysis task on a GenePattern server}

\description{
Accepts a task name to invoke, marshalls input arguments, invokes the analysis 
task on the GenePattern server, polls for completion, and returns a \code{\link{list()}} of 
[local] filenames of output files returned from the server.  When the global flag \code{runningOnServer}
is set to TRUE, the result files are not actually retrieved from the server, only a list of their
names and locations on the server.  They can be retrieved later using \code{\link{getResults(jobID)}}.

As a performance enhancement for pipeline execution, the task name-to-taskID mapping is carried out just once
if the global variable \code{taskArray} is set to the output from \code{\link{getTasks}}.
}

\usage{
filenames <- runAnalysis(taskName, server=defaultServer, ...)
}
\arguments{
  \item{taskName}{ 
  	The name of the task to execute on the GenePattern server.  The name must be accessible to the
	current user -- either as a public task or private but owned by the current user.
  }
  \item{server}{
  	The GenePattern server on which to run the task.  The server is either a URL ("http://serverName:portNumber")
	or is an instance of a \code{\link{SOAPServer}} object.  If no server is specified, the global \code{defaultServer}
	is used.
  }
  \item{...}{
  	All of the arguments required for the the named task to execute on the named server.  Task-optional arguments
	need not be specified.  Arguments set to NULL will be treated as missing (not sent to the server), so they are
	distinct from those set to "".  Non-optional arguments that are not provided will result in an error response
	from the GenePattern server when the task execution is attempted, which will be returned as a file named "stderr"
	in the results filename list.
 }
}
\details{
	runAnalysis performs the following functions:
	\tabular{rl}{
		[,1] \tab convert the server name to normalized form if necessary\cr
		[,2] \tab lookup taskID correspondance to task name on server\cr
		[,3] \tab submit the job to the GenePattern server\cr
		[,4] \tab wait for the server to execute the task and get the completion status\cr
		[,5] \tab if the runningOnServer flag isn't set, retrieve the result files from the server and store them locally\cr
		[,6] \tab return the list of filename results to the caller\cr
	}
}
\value{
	Returns a list of filenames of output files which have been copied to the local computer disk, or in the 
	case of the global \code{runningOnServer} flag == TRUE, a list of filenames on the server.  When the files are
	local, they may be viewed using \code{\link{file.show(filename)}}.  Files remaining on the server may be retrieved
	and displayed using \code{link{readFile(getResults(lastJobID)@filenames[[1]])}} (iterate from 1 to length(getResults(lastJobID))).
	Each returned item in the list has an R name which is the basename of the file, and whose value is the full path to the file.
}
\author{Jim Lerner, Whitehead Center for Genomics Research}

\examples{
	\dontrun{results <- runAnalysis("Threshold", min="10", max="16000", input.filename="nci60.res", server="http://gpserver.foo.com:8080")}
}
\references{
	GenePattern home: \url{http://www-genome.wi.mit.edu/cancer/software/genepattern/}
	GenePattern help: \email{gp-help@genome.wi.mit.edu}
}
}
\keyword{programming}
