library(butler)
library(GenePattern)

gp.server <- setup()
setup <-
function() {
	servername <- "http://localhost:8080"
    username <- "GenePattern"
    password <- "GenePattern"
    gp.server <- gp.login(servername, username, password=password)
    return(gp.server)
}


test.invalid.module.run.analysis <-
function()
{
    module.or.lsid <- "XXXDataset"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
    # Verify exception is thrown
	assert.error(run.analysis(gp.server, module.or.lsid, input.filename=filename))
}

test.valid.module.run.analysis <-
function()
{
    module.or.lsid <- "PreprocessDataset"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
	e <- get_error(run.analysis(gp.server, module.or.lsid, input.filename=filename))
	assert.equal(e, NA)
}

test.optional.params.run.analysis <-
function()
{
    module.or.lsid <- "PreprocessDataset"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
    out.file.format <- "gct"
	e <- get_error(run.analysis(gp.server, module.or.lsid, input.filename=filename, output.file.format=out.file.format))
	assert.equal(e, NA)
}

test.run.visualizer <-
function()
{
    module.or.lsid <- "HeatMapViewer"
    filename <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
	e <- get_error(run.visualizer(gp.server, module.or.lsid, dataset.filename=filename))
	assert.equal(e, NA)
}

test.get.parameters <-
function()
{
    module.or.lsid <- "PreprocessDataset"
    e <- get_error(get.parameters(gp.server, module.or.lsid))
    assert.equal(e, NA)
}

path <- '/Users/jgould/Documents/workspace/gp/test/R/RClientTest.R'
test(path)

