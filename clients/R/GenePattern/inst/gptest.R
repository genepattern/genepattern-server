options(echo = TRUE)
library("methods", character.only=TRUE)
source("gpservices.R")

# Default settings
defaultServer <- SOAPServer("http://localhost", "/axis/servlet/AxisServlet", 8081) #16201

demo <- 
function()
{
# 1.5 MB
#	dataset <- "http://www-genome.wi.mit.edu/mpr/publications/projects/Leukemia/T-ALL_PURE_CASES_11-22-00.res"
# 6.2 MB
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
	excluded <- excludeRows(filename=subset, low=0, high=0, min.fold=1, min.difference=0)

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
	# Geneweaver(filename="c:\\local\\Prostate_BiopsyTrial_ams.res", subsampling="subsample0.8", iterations=10, consensus.clusters=3)
	Geneweaver(filename="c:\\local\\ucGCM_9_15000_All_SOM_2.gct", subsampling="subsample0.8", iterations=10, consensus.clusters=3)
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
function(data, filename, server=defaultServer)
{
	result <- runAnalysis("Echo", sequence=data, filename=filename, server=server)@filenames[[1]]
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
transposeID <- submitJob("Transpose", filename="short.res", taskID=taskID)

taskID <- tasks[match("ID", rownames(tasks)), match("Threshold", colnames(tasks))]
thresholdID <- submitJob("Threshold", filename="short.res", taskID=taskID, min=20, max=16000)

taskID <- tasks[match("ID", rownames(tasks)), match("ExcludeRows", colnames(tasks))]
excludeRowsID <- submitJob("ExcludeRows", filename="short.res", taskID=taskID, low=0, high=0, "min_fold"=1, "min_difference"=0)

taskID <- tasks[match("ID", rownames(tasks)), match("HTMLHeatMap", colnames(tasks))]
HTMLHeatMapID <- submitJob("HTMLHeatMap", filename="short.res", taskID=taskID, "color"=0, "orientation"=0)

taskID <- tasks[match("ID", rownames(tasks)), match("Echo", colnames(tasks))]
echoID <- submitJob("Echo", filename="short.res", taskID=taskID)

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

putFile <- 
#
# return string filename where file is stored on server
#
function(filename, server=defaultServer)
{

}

getFile <-
#
# retrieve file from server based on filename
#
{
}
