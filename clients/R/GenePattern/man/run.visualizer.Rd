\name{run.visualizer}
\alias{run.visualizer}
\title{Runs an visualizer on a GenePattern server}

\description
{
	Accepts a visualizer name or lsid to invoke, and runs the visualizer locally
}

\usage{filenames <- run.visualizer(gp.server, module.name.or.lsid, ...)}

\arguments
{
	\item{gp.server}
	{
 		The URL (i.e.: "http://serverName:portNumber") of GenePattern server on which to run the module.
  	}
  	\item{module.name.or.lsid}
	{
  		The name or lsid of the visualizer to execute on the GenePattern server.  The module must be accessible to the
		current user -- either as a public module or if private must be owned by the current user.
  	}
  	\item{...}
	{
  		All of the arguments required for the the named visualizer to execute on the named server. Visualizer-optional arguments
		do need not to be specified. Arguments set to NULL will be treated as missing (not sent to the server), so they are
		distinct from those set to "". Non-optional arguments that are not provided will result in an error response
		from the GenePattern server when the visualizer execution is attempted.
	}
}

\details
{
	run.visualizer performs the following functions:
  	\tabular{rl}
	{
		[,1] \tab retrive the visualizer from the GenePattern server\cr
		[,2] \tab run the visualizer on the the client machine
  	}
}

\value{Runs a GenePattern visualizer on the client machine}

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
      	## run the HeatMapViewer locally
   		jobresults <- run.visualizer(gp.server, "HeatMapViewer", dataset.filename=input.ds)
  	}
}

\references
{
	GenePattern home: \url{http://www.broad.mit.edu/cancer/software/genepattern/}
	GenePattern help: \email{gp-help@broad.mit.edu}
}

\keyword{genepattern}
\keyword{programming}
