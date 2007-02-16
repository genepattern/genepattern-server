test.invalid.task.run.analysis <-
function()
{
    servername <- "http://localhost:8080"
    username <- "GenePattern"
    password <- NULL
    gp.server <- gp.login(servername, username)
    module.or.lsid <- "InvalidTask"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
    # Verify exception is thrown
	checkException(run.analysis(gp.server, module.or.lsid, input.filename=filename), "Exception not found as expected")
}

test.valid.task.run.analysis <-
function()
{
    servername <- "http://localhost:8080"
    username <- "GenePattern"
    password <- NULL
    gp.server <- gp.login(servername, username)
    module.or.lsid <- "PreprocessDataset"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"

    #Verify that no exception is thrown
	error <- get_error(run.analysis(gp.server, module.or.lsid, input.filename=filename))
	if(is.logical(error))
		return(checkEquals(error, NA)) 
	
	return(FALSE)
}

test.optional.params.run.analysis <-
function()
{
    servername <- "http://localhost:8080"
    username <- "GenePattern"
    password <- NULL
    gp.server <- gp.login(servername, username)
    module.or.lsid <- "PreprocessDataset"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
    out.file.format <- 2
    use.log.base.two <- 0

    #Verify that no exception is thrown
	error <- get_error(run.analysis(gp.server, module.or.lsid, input.filename=file, output.file.format=out.file.format, use.log.base.two=0)) 
	if(is.logical(error))
		return(checkEquals(error, NA))

	return(FALSE)
}
