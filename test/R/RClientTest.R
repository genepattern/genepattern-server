test.valid.gp.login <- 
function(servername, username, password)
{
	gp.server <- gp.login(servername, username, password)
	assert(!is.null(gp.server), "Error retrieving a GPServer object")	
}


test.invalid.run.analysis <-
function(gp.server, module.or.lsid, ...)
{
	checkException(run.analysis(gp.server, module.or.lsid, ...), "Exception not found as expected") 
}

test.valid.run.analysis <-
function(gp.server, module.or.lsid, ...)
{ 
	error <- get_error(run.analysis(gp.server, module.or.lsid, ...)) 
	if(is.logical(error))
		return(checkEquals(error, NA)) 
	
	return(FALSE)
}