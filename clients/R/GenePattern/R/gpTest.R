options(echo = TRUE)
library("GenePattern")

# Default settings
defaultServer <<- SOAPServer("http://localhost", "/axis/servlet/AxisServlet", 8080)

demo <- 
function()
{
# 1.5 MB
#	dataset <- "http://www-genome.wi.mit.edu/mpr/publications/projects/Leukemia/T-ALL_PURE_CASES_11-22-00.res"
# 6.2 MB - 6,262,860 bytes
#	dataset <- "http://www-genome.wi.mit.edu/mpr/publications/projects/Global_Cancer_Map/GCM_Test.res"
# 14.6 MB
#	dataset <- "http://www-genome.wi.mit.edu/mpr/publications/projects/Global_Cancer_Map/GCM_Training.res"
#	dataset <- "Prostate_nonrecur_vs_recur_scaled.res"
	dataset <- "short.res"
# throw out values < 20 or > 100
	cat("Thresholding", dataset, "between 20 and 16000...\n")
	flush.console()
	subset <- threshold(dataset)

# throw out those with a max-min difference < 100 or a max/min < 2
	cat("excluding rows...\n")
	flush.console()
	excluded <- excludeRows("input_filename"=subset, low=0, high=0, min.fold=1, min.difference=0)

# plot the data
	subset <- resultAsGCT((excluded))
	t2 <- subset@data[,2:dim(subset@data)[2]]
	gene.names <- substr(names(t2[,1]),1,4)
	plot(gene.names,t2[,1], col="black", main="expression intensity", xlab="sample", ylab="intensity", type="l")
	lines(gene.names, t2[,2],type="l",col="red")
	lines(gene.names, t2[,3],type="l",col="green")


	cat("displaying ", excluded, " in colorgram...\n", sep="")
	flush.console()
	colorgram(excluded)

	cat("done!\n")
	flush.console()
}

testGeneweaver <- function()
{
	# Geneweaver("input_filename"="c:\\local\\Prostate_BiopsyTrial_ams.res", subsampling="subsample0.8", iterations=10, consensus.clusters=3)
	Geneweaver("input_filename"="c:\\local\\ucGCM_9_15000_All_SOM_2.gct", subsampling="subsample0.8", iterations=10, consensus.clusters=3)
}

testBlast <- function()
{
	Blast(e="1", m="pairwise", p="blastp", d="nr(peptide)", 
		sequence=">gi|2501594|sp|Q57997|Y577_METJA PROTEIN MJ0577\r\nMSVMYKKILYPTDFSETAEIALKHVKAFKTLKAEEVILLHVIDEREIKKRDIFSLLLGVAGLNKSVEEFENELKNKLTEEAKNKMENIKKELEDVGFKVKDIIVVGIPHEEIVKIAEDEGVDIIIMGSHGKTNLKEILLGSVTENVIKKSNKPVLVVKRKNS")
}

testBlat <- function()
{
return (Blat(sequence=">test\r\nTACAGGGTGCGGGTTCCAGGAGTCTGCCTAGAAGGCAAAAAACAGGCTTTGCTTAGAATCCCCTAAATTGCTCATAAAACA", database="HG11"))
}

echo <-
#
# test the GenePattern echo task
#
function(filename, server=defaultServer)
{
	result <- runAnalysis("echo", "input_filename"=filename, server=server)[[1]]
	cat(readFile(result))
	file.remove(result)
}

multipleThreads <-
#
# test running GP multiple threads concurrently
#
function()
{
tasks <- getTasks()
if (is.null(tasks)) stop("unable to retrieve list of available tasks from ", server@host, ":", server@port);

taskID <- tasks[match("ID", rownames(tasks)), match("Transpose", colnames(tasks))]
transposeID <- submitJob("Transpose", "input_filename"="short.res", taskID=taskID)

taskID <- tasks[match("ID", rownames(tasks)), match("Threshold", colnames(tasks))]
thresholdID <- submitJob("Threshold", "input_filename"="short.res", taskID=taskID, min=20, max=16000)

taskID <- tasks[match("ID", rownames(tasks)), match("ExcludeRows", colnames(tasks))]
excludeRowsID <- submitJob("ExcludeRows", "input_filename"="short.res", taskID=taskID, low=0, high=0, "min_fold"=1, "min_difference"=0)

taskID <- tasks[match("ID", rownames(tasks)), match("HTMLHeatMap", colnames(tasks))]
HTMLHeatMapID <- submitJob("HTMLHeatMap", "input_filename"="short.res", taskID=taskID, "color"=0, "orientation"=0)

taskID <- tasks[match("ID", rownames(tasks)), match("echo", colnames(tasks))]
echoID <- submitJob("Echo", "input_filename"="short.res", taskID=taskID)

cat("transpose results\n")
flush.console()
result <- waitForResults(transposeID)
cat(as.character(readFile(result)))
if (class(result) == "result" && length(result@filenames) > 0) file.remove(unlist(result@filenames))

cat("threshold results\n")
flush.console()
result <- waitForResults(thresholdID)
cat(as.character(readFile(result)))
if (class(result) == "result" && length(result@filenames) > 0) file.remove(unlist(result@filenames))

cat("exclude rows results\n")
flush.console()
result <- waitForResults(excludeRowsID)
cat(as.character(readFile(result)))
if (class(result) == "result" && length(result@filenames) > 0) file.remove(unlist(result@filenames))

cat("HTMLHeatMap in browser\n")
flush.console()
result <- waitForResults(HTMLHeatMapID)
if (class(result) == "result" && length(result@filenames) > 0) {
	browseURL(result@filenames[[1]])
	Sys.sleep(3)
	file.remove(unlist(result@filenames))
}

cat("echo results\n")
flush.console()
result <- waitForResults(echoID)
cat(as.character(readFile(result)))
if (class(result) == "result" && length(result@filenames) > 0) file.remove(unlist(result@filenames))
flush.console()

}

appender <-
#
# append N files together and return them as one
#
function(..., server=defaultServer)
{
	filenames <- lapply(list(...), function(filename) { return (putFile("input_filename"=filename, server=server)) })
	cat(unlist(filenames))
}

test <- function() { return (appender("c:/local/short.res", "c:/autoexec.bat", "c:/local/updateJars.bat")) }

testRGPTM <-
#
#
#
function()
{
	className <- "edu.mit.gp.ui.pinkogram.BpogPanel"
	taskName <- "BluePinkOGram"
	host <- defaultServer@host
	port <- defaultServer@port
	classPath <- "<libdir>gp.jar;<libdir>trove.jar;<libdir>openide.jar;<libdir>crimson.jar" 
	runGenePatternTaskMain(className, taskName, host, port, classPath, "c:\\local\\short.res")
}

Merge2 <-
#
# merge datasets
#
# input parameters:
#	output.name:  base name of output files
#	normalization:  different normalization options
#	             choose from the following values: 0,1,2,3
#	global.shift:  global additive constant
# returns:
#	filename of results file
# author:
#	Jim Lerner with Pablo Tamayo
#
function(..., output.name, normalization=0, global.shift=0, server=defaultServer)
{
	filenames <- putFiles(..., server=server) #upload files to server

	# write the filenames and descriptive information to a file, then submit that as the attachment
	descriptorFilename <- paste(output.name, ".in", sep="")
	f <- file(descriptorFilename, "w")
	cat("#Desc\ttformat\tmapdir\tfflag\tthres\tceil\tfdiff\tadiff\tnexcl\tFname\ttname\n", file=f)
	cat(unlist(lapply(seq(filenames), function(fnum) { return(paste("D", fnum, "\t0\t0\t0\t0\t0\t0\t0\t0\t", filenames[[fnum]], "\tNULL\n", sep="")) })), sep="", file=f)
	close(f)

	result <- runAnalysis("Merge2", "input_filename"=descriptorFilename, "output_name"=output.name, 
			    "normalization"=normalization, "global_shift"=global.shift, 
			    server=server)
	file.remove(descriptorFilename);
	return (result)
}

merge3 <-
#
# merge datasets
#
# input parameters:
#	output.name:  base name of output files
#	normalization:  different normalization options
#	             choose from the following values: 0,1,2,3
#	global.shift:  global additive constant
# returns:
#	filename of results file
# author:
#	Jim Lerner with Pablo Tamayo
#
function(..., output.name=winDialogString("output_name (base name of output files)",""),
	 normalization=0, global.shift=0, server=defaultServer)
{
	filenames <- putFiles(..., server=server) #upload files to server

	# write the filenames and descriptive information to a file, then submit that as the attachment
	descriptorFilename <- paste(output.name, ".in", sep="")
	f <- file(descriptorFilename, "w")
	cat("#Desc\ttformat\tmapdir\tfflag\tthres\tceil\tfdiff\tadiff\tnexcl\tresFname\tclsFname\ttname\n", file=f)
	cat(unlist(lapply(seq(filenames), function(fnum) { return(paste("D", fnum, "\t0\t0\t0\t0\t0\t0\t0\t0\t", filenames[[fnum]], "\tNULL\n", sep="")) })), sep="", file=f)
	close(f)

	result <- runAnalysis("merge3", "input_filename"=descriptorFilename, "output_name"=output.name, 
			    "normalization"=normalization, "global_shift"=global.shift, 
			    server=server)
	file.remove(descriptorFilename);
	return (result)
}

change.features <-
#
# change the features of a pair of datasets
#
# input parameters:
#	res.file:  Input RES file
#	lookup.table:  Name of the look up table file
#	direction:  Direction of look up: 1=direct (default), 0=reverse
#	            choose from the following values: 1,0
#	table.format:  Table format: 0=2-column or 1=4-column table
#	             choose from the following values: 0,1
#	output.name:  output filename
# returns:
#	filename of results file
# author:
#
function(filename, lookup.table, direction=1, table.format=0, output.name, 
	 server=defaultServer)
{
	lookupFilename <- putFile(lookup.table, server=server) #upload files to server
	return (runAnalysis("change_features", "input_filename"=filename, 
			    "lookup_table"=lookupFilename, "direction"=direction, 
			    "table_format"=table.format, "output_name"=basename(output.name), 
			    server=server))
}

# f <- change.features("c:/temp/change_features/ALL_vs_AML_toy.1.res", "c:/temp/change_features/Affy_table.txt", direction=1, table.format=0, output.name="cf.res")