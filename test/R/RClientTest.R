test.valid.gp.login <- 
function(servername, username, password)
{
	gp.server <- gp.login(servername, username, password)
	assert(!is.null(gp.server), "Error retrieving a GPServer object")	
}



