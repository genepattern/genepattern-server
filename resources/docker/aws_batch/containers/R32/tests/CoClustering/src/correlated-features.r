


## for parsing GenePattern inputs
suppressMessages(suppressWarnings(library(getopt)))
suppressMessages(suppressWarnings(library(optparse)))


## libraries needed by this module
suppressMessages (suppressWarnings (library (cluster)))
suppressMessages (suppressWarnings (library (ape)))
suppressMessages (suppressWarnings (library (RColorBrewer)))


sessionInfo ()



remove.last3 <- function (x) {
  # remove last 3 elements of a vector
  n <- length(x)
  x <- x [ -((n-2):n)]
  return (x)
}


gene.filter <- function (prot, rna, genes.keep) {
  prot <- prot [ prot[,1] %in% genes.keep, ]
  rna <- rna [ rna[,1] %in% genes.keep, ]
  joint <- merge (prot, rna, by='GeneSymbol', suffixes=c('.P', '.R'))
  return (joint)
}


cluster.and.plot <- function (joint, suffix, subtitle) {
  ## cluster RNA+protein to see if the samples co-cluster
  #  (distance = 1 - spearman correlation)
  dist.spearman <- 1 - cor (joint[,-1], method="spearman", use="complete")
  joint.cluster <- agnes (dist.spearman, diss=TRUE, method='complete')
  pdf (paste ('joint-hclust-', suffix, '.pdf', sep=''), width=18, height=4, pointsize=8)
  plot (joint.cluster, which.plots=2, main="Co-clutering RNA and Protein",
        sub=subtitle, xlab='Samples', cex=1)
  dev.off()
  
  # plot with color, and more compactly
  joint.phy <- as.phylo (as.hclust (joint.cluster))
  samplecolors <- c ( brewer.pal (8, name='Dark2'), brewer.pal (9, name='Set1')[-6] )
  clustercolors <- c (clu, clu)
  pam50colors <- c (cls, cls)
  RPcolors <- rep (c ('grey80', 'grey40'), each=length(cls))  #rep (brewer.pal (3, name='Set2')[1:2], each=length(cls))
  edgecolors <- sapply  (joint.phy$edge[,2], 
                         function (x) ifelse (x>160, rgb(0,0,0), c (cls, cls)[x]))
      
  pdf (paste ('joint-fanplot-', suffix, '.pdf', sep=''), width=8, height=8)
  plot (joint.phy, type='fan', cex=0.5, tip.color=samplecolors, edge.color="black", 
        font=1, label.offset=0.02, edge.width=1.5, y.lim=c(-1.15,1.15))
  # ring (0.05, joint.phy, col=samplecolors, offset=0.25)
  ring (0.05, joint.phy, col=RPcolors, offset=0.25)
  # ring (0.05, joint.phy, col=pam50colors, offset=0.3)
  # ring (0.05, joint.phy, col=clustercolors, offset=0.35)
  dev.off()
  
  # determine number of adjacent RNA-protein pairs
  samples <- colnames (joint[,-1])[as.hclust (joint.cluster)$order]
  adjpairs <- 0
  i <- 1
  while (i < length(samples)) {
    if (grepl (substr (samples[i], 1, 7), samples[i+1], fixed=TRUE)) {
      adjpairs <- adjpairs + 1
      i <- i+1
    }
    i <- i+1
  }
  cat ('\n\nRNA-Protein pairs (', suffix, ') :', adjpairs, '\n')
  cat ('Input dataset:', nrow(joint), 'x', ncol(joint)-1, '\n\n')
  
  return (adjpairs)
}



# Get the command line arguments.  We'll process these with optparse.
# https://cran.r-project.org/web/packages/optparse/index.html
arguments <- commandArgs(trailingOnly=TRUE)

# Declare an option list for optparse to use in parsing the command line.
option_list <- list(
  # Note: it's not necessary for the names to match here, it's just a convention
  # to keep things consistent.
  make_option("--libdir", dest="libdir"),
  make_option("--min.correlation", dest="min.correlation", type='numeric')
)

# Parse the command line arguments with the option list, printing the result
# to give a record as with sessionInfo.
opt <- parse_args (OptionParser (option_list=option_list), positional_arguments=TRUE, args=arguments)
print (opt)
opts <- opt$options

## read in supporting functions
source (file.path (opts$libdir, "io.r"))

# colors for PAM-50 classes
# (to mimic original TCGA BRCA Nature paper)
# in order: Basal, Her2, LumA, LumB, non-BRCA-class
colors.pam50 <- list (Basal=rgb(0.8,0,0.2), Her2=rgb(1,0.6,1), LumA=rgb(0.2,0.2,1),
                      LumB=rgb(0.2,0.8,1), Luminal=rgb(0.2,0.5,1),
                      nonBRCA=rgb(0,0.6,0), other=rgb(0.5,0.5,0.4))

## read in data tables and preprocess
prot.file <- file.path (opts$libdir, 'proteome-matrix.csv')
rna.file <- file.path (opts$libdir, 'rnaseq-matrix.csv')

# read cls files and map PAM-50 / cluster labels to colors
cls <- read.cls (file.path (opts$libdir, 'proteome-unimodal-pam50.cls'))
cls <- remove.last3 (cls)
for (i in 1:length (colors.pam50))
  cls [cls==names(colors.pam50)[i]] <- colors.pam50[[i]]

clu <- read.cls (file.path (opts$libdir, 'cluster.cls'))
clu <- remove.last3 (clu)
clu.colors <- brewer.pal (3, name='Set1')
clus <- unique (clu)
for (i in 1:length(clus)) 
  clu [clu==clus[i]] <- clu.colors[i]


# datasets
prot <- read.csv (prot.file, check.names=FALSE)
rna <- read.csv (rna.file,  check.names=FALSE)
colnames (rna) <- make.unique (colnames(rna))
rna <- rna [, colnames (prot)]

# filter by RNA-protein correlation
cor.min.list <- c (opts$min.correlation)
cor.table <- read.delim (file.path (opts$libdir, 'proteome-unimodal-mrna-cor.tsv'))
genes <- prot [,1]
for (cor.min in cor.min.list) {
  cor.genes <- cor.table [ cor.table[,'correlation'] > cor.min, 'gene_name']
  genes.cor <- intersect (genes, cor.genes)
  joint.cor <- gene.filter (prot, rna, genes.cor)
  pairs.cor <- cluster.and.plot (joint.cor, suffix=paste ('cor', cor.min, sep=''), 
                                 subtitle=paste ("RNA-protein correlation >", cor.min))
}
  
