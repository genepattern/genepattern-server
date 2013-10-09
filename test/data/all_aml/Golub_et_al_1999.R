#
#  GenePattern Methodology for:
#
#  Molecular Classification of Cancer: Class Prediction by Gene Expression 
#
#  Summary: This R/GenePattern script implements a methodology similar to the
#           supervised learning method used in Golub et al 1999, Science 286:531-537 (1999).
#
#  Author:   Pablo Tamayo (tamayo@genome.wi.mit.edu)
#  Date:     June 30, 2003
#
#  Execute from an R console window with this command:
#  source("<this file>", echo = TRUE)
#
#  This methodology involves the following computational tasks
#  
#   1) Preprocess the train and test datasets (thresholding and filtering)
#   2) Select the marker genes most correlated with the ALL vs. AML distinction (show plot)
#   3) Build a cross-validation classifier (Weighted Voting) on the train set
#   4) Buiild a classifier (Weighted Voting) on the train set and applied to an independent test set
#

#  Load GenePattern library and set server

library(GenePattern)
server <- "http://localhost:8080"
gpTasksAsFunctions()

# Start of methodology

# Needed datasets

train.ds <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res"
train.cls <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.cls"

test.ds <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_test.res"
test.cls <- "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_test.cls"

# Neighborhood analysis: selecting gene markers for the Leukemia subclasses (ALL/AML)
 
MS.out <- ClassNeighbors(data.filename = train.ds, class.filename = train.cls, marker.gene.list.file = "marker.results",
                         marker.data.set.file = "data.results", num.permutations = "250", user.pval = "0.5", num.neighbors = "2000", ttest.or.snr="-S",
                         server = server, filter.data="yes",min.threshold="10", max.threshold="16000", min.fold.diff="5", min.abs.diff="50")
# Display marker list

file.show(MS.out$marker.results, title = "Gene marker list")

# Read marker list and make neighborhood plot

par(mfrow=c(2,2))

data <- read.delim(MS.out$marker.results, as.is=T, header=F, sep="\t", skip=14)
data2 <- as.matrix(data)
cols <- length(data2[1,]) 
rows <- length(data2[,1])/2
i1row <- 1
e1row <- rows
i2row <- rows + 1
e2row <- 2*rows

obs <- -as.numeric(data2[i1row:e1row,3])
y <- as.numeric(data2[i1row:e1row,1]) 
pt1pct <- -as.numeric(data2[i1row:e1row,8])
pt5pct <- -as.numeric(data2[i1row:e1row,9])
pt50pct <- -as.numeric(data2[i1row:e1row,10])
plot(obs, y, xlim=c(-2, 0), ylim=c(1, rows), main="Markers of ALL", xlab="Measure of Correlation", ylab="Genes", type="n", log="y")
points(obs, y, pch=8, type="p",cex = 0.8, bg = "black", col = "black")
lines(pt1pct, y, type="l", col = "blue")
lines(pt5pct, y, type="l", col = "green")
lines(pt50pct, y, type="l", col = "brown")
leg.txt <- c("observed", "Permuted: 1%", "Permuted: 5%", "Permuted: 50%")
legend(x=-2, y=rows, legend=leg.txt, bty="n", pch = c("*", "_", "_", "_"), col = c("black", "blue", "green", "brown"), cex = 0.8)

obs <- -as.numeric(data2[i2row:e2row,3])
y <- as.numeric(data2[i2row:e2row,1]) - rows
pt1pct <- -as.numeric(data2[i2row:e2row,8])
pt5pct <- -as.numeric(data2[i2row:e2row,9])
pt50pct <- -as.numeric(data2[i2row:e2row,10])
plot(obs, y, xlim=c(-2, 0), ylim=c(1, rows), main="Markers of AML", xlab="Measure of Correlation", ylab="Genes", type="n", log="y")
points(obs, y, pch=8, type="p",cex = 0.8, bg = "black", col = "black")
lines(pt1pct, y, type="l", col = "blue")
lines(pt5pct, y, type="l", col = "green")
lines(pt50pct, y, type="l", col = "brown")
leg.txt <- c("observed", "Permuted: 1%", "Permuted: 5%", "Permuted: 50%")
legend(x=-2, y=rows, legend=leg.txt, bty="n", pch = c("*", "_", "_", "_"), col = c("black", "blue", "green", "brown"), cex = 0.8)

# Class prediction using a weighted voting (WV) classifier 

# Cross-validation of the train dataset
                                       
WV.CV.out <- WeightedVotingXValidation(data.filename = train.ds, class.filename = train.cls, output.file = "train.pred.results", filter.data="yes",
                                       thresh.min = "20", thresh.max = "16000", fold.diff = "5", num.features = "50", server = server)

# Display the WV prediction results

file.show(WV.CV.out$train.pred.results, title = "Cross validation prediction results")

data <- read.delim(WV.CV.out$train.pred.results, as.is=T, header=F, sep="\t", skip=10)
data2 <- as.matrix(data)
cols <- length(data2[1,]) 
rows <- length(data2[,1])
true.class <- as.numeric(data2[,2])
pred.class <- as.numeric(data2[,3])
conf <- as.numeric(data2[,4])
correct <- as.logical(data2[,5])
pred.class.2 <- (pred.class * 2) - 1
signed.conf <- pred.class.2 * conf
ord <- rev(order(signed.conf))
sorted.conf <- signed.conf[ord]
sorted.true.class <- true.class[ord]
sorted.pred.class <- pred.class[ord]
sorted.correct <- correct[ord]
plot(1:rows, sorted.conf, xlim=c(1, rows), ylim=c(-1, 1.25), main="CV/Training Set Predictions", xlab="Sample index", ylab="Confidence", type="n")
linex <- c(0, rows)
liney <- c(0, 0)
lineyup <- c(0.3, 0.3)
lineydown <- c(-0.3, -0.3)
lines(linex, liney, type="l", col = "black")
lines(linex, lineyup, type="l", col = "grey")
lines(linex, lineydown, type="l", col = "grey")
for (i in 1:rows) {
   if (sorted.true.class[i] == 1) col = "green"
   else col = "blue"
   points(i, sorted.conf[i], pch=22, type="p", cex = 1.25, bg = col, col = "black")
   if (sorted.correct[i] == F) points(i, 1.2, pch=8, type="p", cex = 1, bg = "black", col = "black")
}
legend(x=25, y=0.9, legend=c("AML", "ALL"), bty="n", pch = c(22, 22), col = c("black", "black"), pt.bg = c("green", "blue"), cex = 1.25)

# Classification of the test set 

WV.out <- WeightedVoting(train.filename = train.ds, train.class.filename = train.cls, test.filename = test.ds,  
                         test.class.filename = test.cls, pred.results.file = "test.pred.results", thresh.min = "20", thresh.max ="16000", filter.data="yes",
                         fold.diff = "5", absolute.diff = "100", num.features = "50", server = server)

# Display the WV prediction results

file.show(WV.out$test.pred.results, title = "Test dataset prediction results")

data <- read.delim(WV.out$test.pred.results, as.is=T, header=F, sep="\t", skip=10)
data2 <- as.matrix(data)
cols <- length(data2[1,]) 
rows <- length(data2[,1])
true.class <- as.numeric(data2[,2])
pred.class <- as.numeric(data2[,3])
conf <- as.numeric(data2[,4])
correct <- as.logical(data2[,5])
pred.class.2 <- (pred.class * 2) - 1
signed.conf <- pred.class.2 * conf
ord <- rev(order(signed.conf))
sorted.conf <- signed.conf[ord]
sorted.true.class <- true.class[ord]
sorted.pred.class <- pred.class[ord]
sorted.correct <- correct[ord]
plot(1:rows, sorted.conf, xlim=c(1, rows), ylim=c(-1, 1.25), main="Independent/Test Set Predictions", xlab="Sample index", ylab="Confidence", type="n")
linex <- c(0, rows)
liney <- c(0, 0)
lineyup <- c(0.3, 0.3)
lineydown <- c(-0.3, -0.3)
lines(linex, liney, type="l", col = "black")
lines(linex, lineyup, type="l", col = "grey")
lines(linex, lineydown, type="l", col = "grey")
for (i in 1:rows) {
   if (sorted.true.class[i] == 1) col = "green"
   else col = "blue"
   points(i, sorted.conf[i], pch=22, type="p", cex = 1.25, bg = col, col = "black")
   if (sorted.correct[i] == F) points(i, 1.2, pch=8, type="p", cex = 1, bg = "black", col = "black")
}
legend(x=25, y=0.9, legend=c("AML", "ALL"), bty="n", pch = c(22, 22), col = c("black", "black"), pt.bg = c("green", "blue"), cex = 1.25)

# End of methodology
