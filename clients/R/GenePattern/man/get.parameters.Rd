\name{get.parameters}
\alias{get.parameters}
\title{Returns the list of parameters for a given module name or lsid on a GenePattern server}

\description
{
	Accepts the name or lsid of a module and returns a list of all the parameters specified for it on the GenePattern
	server. They can be used later to call the invoke that module using \code{\link{run.analysis(gp.server, parameter1=value1, parameter2=value2, etc.)}}.
}

\usage{filenames <- get.parameters(gp.server, module.name.or.lsid)}

\arguments
{
	\item{gp.server}
	{
 		The URL (i.e.: "http://serverName:portNumber") of GenePattern server on which to run the module.
  	}
  	\item{module.name.or.lsid}
	{
  		The name or lsid of the module whose parameters to obtain from the GenePattern server.
  		The module must be accessible to the current user -- either as a public module or if private must be owned by the current user.
  	}
}

\details
{
	get.parameters performs the following functions:
  	\tabular{rl}
	{
		[,1] \tab submit module parameter query to the GenePattern server\cr
		[,2] \tab return an R list of the parameters specified for the module \cr
  	}
}

\value{Returns an R list of the parameters for the module.}

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
      	## get the parameters for the the TransposeDataset analysis on the GenePattern server
   		parameter.list <- get.parameters(gp.server, "TransposeDataset")
  	}
}

\references
{
	GenePattern home: \url{http://www.broad.mit.edu/cancer/software/genepattern/}
	GenePattern help: \email{gp-help@broad.mit.edu}
}

\keyword{genepattern}
\keyword{programming}