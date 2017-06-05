#
#  REVEALER Libray of functions
#  Kim & Botvinnik, et al. Characterizing Genomic Alterations in Cancer by Complementary Functional Associations
#  Nature Biotechnology 2016
#

# Check if all neccessary R packages are installed and if not install those missing

#
#  REVEALER Libray of functions
#  Kim & Botvinnik, et al. Characterizing Genomic Alterations in Cancer by Complementary Functional Associations
#  Nature Biotechnology 2016
#

# Check if all neccessary R packages are installed and if not install those missing

#   list.of.packages <- c("misc3d", "MASS", "smacof", "NMF", "RColorBrewer", "ppcor", "maptools")
#   list.of.packages <- c("misc3d", "MASS", "NMF", "RColorBrewer", "ppcor", "maptools")
#   new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
#   if(length(new.packages)) install.packages(new.packages)

  # Check if all neccessary packages are installed and if not install those missing

   suppressPackageStartupMessages(library(misc3d))
   suppressPackageStartupMessages(library(MASS))
#   suppressPackageStartupMessages(library(smacof))
   suppressPackageStartupMessages(library(NMF))
   suppressPackageStartupMessages(library(RColorBrewer))
   suppressPackageStartupMessages(library(ppcor))
   suppressPackageStartupMessages(library(maptools))
   suppressPackageStartupMessages(library(optparse))

  ### Revealer 2.0  February 6, 2013

  REVEALER.v1 <- function(
   # REVEALER (Repeated Evaluation of VariablEs conditionAL Entropy and Redundancy) is an analysis method specifically suited
   # to find groups of genomic alterations that match in a complementary way, a predefined functional activation, dependency of
   # drug response “target” profile. The method starts by considering, if available, already known genomic alterations (“seed”)
   # that are the known “causes” or are known or assumed “associated” with the target. REVEALER starts by finding the genomic
   # alteration that best matches the target profile “conditional” to the known seed profile using the conditional mutual information.
   # The newly discovered alteration is then merged with the seed to form a new “summary” feature, and then the process repeats itself
   # finding additional complementary alterations that explain more and more of the target profile.

   ds1,                                          # Dataset that contains the "target"
   target.name,                                  # Target feature (row in ds1)
   target.match = "positive",                    # Use "positive" to match the higher values of the target, "negative" to match the lower values
   ds2,                                          # Features dataset.
   seed.names = NULL,                            # Seed(s) name(s)
   exclude.features = NULL,                      # Features to exclude for search iterations
   max.n.iter = 5,                               # Maximun number of iterations
   pdf.output.file,                              # PDF output file
   count.thres.low = NULL,                       # Filter out features with less than count.thres.low 1's
   count.thres.high = NULL,                      # Filter out features with more than count.thres.low 1's

   n.markers = 30,                               # Number of top hits to show in heatmap for every iteration
   locs.table.file = NULL)                       # Table with chromosomal location for each gene symbol (optional)

{  # Additional internal settings
    
   identifier = "REVEALER"                      # Documentation suffix to be added to output file    
   n.perm = 10                                  # Number of permutations (x number of genes) for computing p-vals and FRDs
   save_preprocessed_features_dataset = NULL    # Save preprocessed features dataset    
   seed.combination.op = "max"                  # Operation to consolidate and summarize seeds to one vector of values
   assoc.metric = "IC"                          # Assoc. Metric: "IC" information coeff.; "COR" correlation.
   normalize.features = F                       # Feature row normalization: F or "standardize" or "0.1.rescaling"
   top.n = 1                                    # Number of top hits in each iteration to diplay in Landscape plot
   max.n = 2                                    # Maximum number of iterations to diplay in Landscape plot
   phen.table = NULL                            # Table with phenotypes for each sample (optional)
   phen.column = NULL                           # Column in phen.table containing the relevant phenotype info
   phen.selected = NULL                         # Use only samples of these phenotypes in REVEALER analysis
   produce.lanscape.plot = F                    # Produce multi-dimensional scaling projection plot
   character.scaling = 1.25                     # Character scaling for heatmap
   r.seed = 34578                               # Random number generation seed
   consolidate.identical.features = F           # Consolidate identical features: F or "identical" or "similar" 
   cons.features.hamming.thres = NULL           # If consolidate.identical.features = "similar" then consolidate features within this Hamming dist. thres.

# ------------------------------------------------------------------------------------------------------------------------------
   print(paste("ds1:", ds1))
   print(paste("target.name:", target.name))
   print(paste("target.match:", target.match))
   print(paste("ds2:", ds2))                        
   print(paste("seed.names:", seed.names))
#   print(paste("exclude.features:", exclude.features))
   print(paste("max.n.iter:", max.n.iter))
   print(paste("pdf.output.file:", pdf.output.file))
#   print(paste("n.perm:", n.perm))                  
   print(paste("count.thres.low:", count.thres.low))
   print(paste("count.thres.high:", count.thres.high))
#   print(paste("identifier:", identifier))                  
   print(paste("n.markers:", n.markers))   
   print(paste("locs.table.file:", locs.table.file))

# ------------------------------------------------------------------------------------------------------------------------------
   # Load libraries

   if (is.null(seed.names)) seed.names <- "NULLSEED"
   
   pdf(file=pdf.output.file, height=14, width=8.5)
   set.seed(r.seed)

   # Read table with HUGO gene symbol vs. chr location
   
   if (!is.null(locs.table.file)) {
      locs.table <- read.table(locs.table.file, header=T, sep="\t", skip=0, colClasses = "character")
    }
   
   # Define color map

   mycol <- vector(length=512, mode = "numeric")
   for (k in 1:256) mycol[k] <- rgb(255, k - 1, k - 1, maxColorValue=255)
   for (k in 257:512) mycol[k] <- rgb(511 - (k - 1), 511 - (k - 1), 255, maxColorValue=255)
   mycol <- rev(mycol)
   ncolors <- length(mycol)

   # Read datasets

   dataset.1 <- MSIG.Gct2Frame(filename = ds1)
   m.1 <- data.matrix(dataset.1$ds)
   row.names(m.1) <- dataset.1$row.names
   Ns.1 <- ncol(m.1)  
   sample.names.1 <- colnames(m.1) <- dataset.1$names

   dataset.2 <- MSIG.Gct2Frame(filename = ds2)
   m.2 <- data.matrix(dataset.2$ds)
   row.names(m.2) <- dataset.2$row.names
   Ns.2 <- ncol(m.2)  
   sample.names.2 <- colnames(m.2) <- dataset.2$names

    # exclude samples with target == NA

   target <- m.1[target.name,]
   print(paste("initial target length:", length(target)))      
   locs <- seq(1, length(target))[!is.na(target)]
   m.1 <- m.1[,locs]
   sample.names.1 <- sample.names.1[locs]
   print(paste("target length after excluding NAs:", ncol(m.1)))     

   overlap <- intersect(sample.names.1, sample.names.2)
   length(overlap)
   locs1 <- match(overlap, sample.names.1)
   locs2 <- match(overlap, sample.names.2)
   m.1 <- m.1[, locs1]
   m.2 <- m.2[, locs2]

   # Filter samples with only the selected phenotypes 

   if (!is.null(phen.selected)) {
      samples.table <- read.table(phen.table, header=T, row.names=1, sep="\t", skip=0)
      table.sample.names <- row.names(samples.table)
      locs1 <- match(colnames(m.2), table.sample.names)
      phenotype <- as.character(samples.table[locs1, phen.column])
      
      locs2 <- NULL
      for (k in 1:ncol(m.2)) {   
         if (!is.na(match(phenotype[k], phen.selected))) {
            locs2 <- c(locs2, k)
         }
      }
      length(locs2)
      m.1 <- m.1[, locs2]
      m.2 <- m.2[, locs2]
      phenotype <- phenotype[locs2]
      table(phenotype)
    }

   # Define target

   target <- m.1[target.name,]
   if (target.match == "negative") {
      ind <- order(target, decreasing=F)
   } else {
      ind <- order(target, decreasing=T)
   }
   target <- target[ind]
   m.2 <- m.2[, ind]

   if (!is.na(match(target.name, row.names(m.2)))) {
     loc <- match(target.name, row.names(m.2))
     m.2 <- m.2[-loc,]
   }

   MUT.count <- AMP.count <- DEL.count <- 0
   for (i in 1:nrow(m.2)) {
      temp <- strsplit(row.names(m.2)[i], split="_")
      temp <- strsplit(temp[[1]][length(temp[[1]])], split=" ")
      suffix <- temp[[1]][1]
      if (!is.na(suffix)) {
         if (suffix == "MUT") MUT.count <- MUT.count + 1
         if (suffix == "AMP") AMP.count <- AMP.count + 1
         if (suffix == "DEL") DEL.count <- DEL.count + 1
     }
   }
   print(paste("Initial number of features ", nrow(m.2), " MUT:", MUT.count, " AMP:", AMP.count, " DEL:", DEL.count))
   
   # Eliminate flat, sparse or features that are too dense

   if (!is.null(count.thres.low) && !is.null(count.thres.high)) {
      sum.rows <- rowSums(m.2)
      seed.flag <- rep(0, nrow(m.2))
      if (seed.names != "NULLSEED") {
         locs <- match(seed.names, row.names(m.2))
         locs <- locs[!is.na(locs)]
         seed.flag[locs] <- 1
      }
      retain <- rep(0, nrow(m.2))
      for (i in 1:nrow(m.2)) {
         if ((sum.rows[i] >= count.thres.low) && (sum.rows[i] <= count.thres.high)) retain[i] <- 1
         if (seed.flag[i] == 1) retain[i] <- 1
      }

      m.2 <- m.2[retain == 1,]
      print(paste("Number of features kept:", sum(retain), "(", signif(100*sum(retain)/length(retain), 3), " percent)"))
  }
   
   # Normalize features and define seeds

  if (normalize.features == "standardized") {
      for (i in 1:nrow(m.2)) {
         mean.row <- mean(m.2[i,])
         sd.row <- ifelse(sd(m.2[i,]) == 0, 0.1*mean.row, sd(m.2[i,]))
         m.2[i,] <- (m.2[i,] - mean.row)/sd.row
       }
   } else if (normalize.features == "0.1.rescaling") {
      for (i in 1:nrow(m.2)) {
         max.row <- max(m.2[i,])
         min.row <- min(m.2[i,])
         range.row <- ifelse(max.row == min.row, 1, max.row - min.row)
         m.2[i,] <- (m.2[i,] - min.row)/range.row
       }
    }
   
  if (seed.names == "NULLSEED") {
     seed <- as.vector(rep(0, ncol(m.2)))      
     seed.vectors <- as.matrix(t(seed))
  } else {
      print("Location(s) of seed(s):")
      print(match(seed.names, row.names(m.2)))
      if (length(seed.names) > 1) {
         seed <- apply(m.2[seed.names,], MARGIN=2, FUN=seed.combination.op)
         seed.vectors <- as.matrix(m.2[seed.names,])
      } else {
         seed <- m.2[seed.names,]
         seed.vectors <- as.matrix(t(m.2[seed.names,]))
      }
      locs <- match(seed.names, row.names(m.2))
      locs
     m.2 <- m.2[-locs,]
     dim(m.2)
   }

  if (length(table(m.2[1,])) > ncol(m.2)*0.5) { # continuous target
     feature.type <- "continuous"
  } else {
     feature.type <- "discrete"
  }
    
  # Exclude user-specified features 
   
   if (!is.null(exclude.features)) {
      locs <- match(exclude.features, row.names(m.2))
      locs <- locs[!is.na(locs)]
      m.2 <- m.2[-locs,]
    }

  #  Consolidate identical features

  # This is a very fast way to eliminate perfectly identical features compared with what we do below in "similar"
   
   if (consolidate.identical.features == "identical") {  
      dim(m.2)
      summary.vectors <- apply(m.2, MARGIN=1, FUN=paste, collapse="")
      ind <- order(summary.vectors)
      summary.vectors <- summary.vectors[ind]
      m.2 <- m.2[ind,]
      taken <- i.count <- rep(0, length(summary.vectors))
      i <- 1
      while (i <= length(summary.vectors)) {
        j <- i + 1
        while ((summary.vectors[i] == summary.vectors[j]) & (j <= length(summary.vectors))) {
            j <- j + 1
         }
        i.count[i] <- j - i
        if (i.count[i] > 1) taken[seq(i + 1, j - 1)] <- 1
        i <- j
      }
   
      if (sum(i.count) != length(summary.vectors)) stop("ERROR")     # Add counts in parenthesis
      row.names(m.2) <- paste(row.names(m.2), " (", i.count, ")", sep="")
      m.2 <- m.2[taken == 0,]
      dim(m.2)

   # This uses the hamming distance to consolidate similar features up to the Hamming dist. threshold 
      
   } else if (consolidate.identical.features == "similar") { 
      hamming.matrix <- hamming.distance(m.2)
      taken <- rep(0, nrow(m.2))
      for (i in 1:nrow(m.2)) {
         if (taken[i] == 0) { 
            similar.features <- row.names(m.2)[hamming.matrix[i,] <= cons.features.hamming.thres]
            if (length(similar.features) > 1) {
               row.names(m.2)[i]  <- paste(row.names(m.2)[i], " [", length(similar.features), "]", sep="") # Add counts in brackets
               locs <- match(similar.features, row.names(m.2))
               taken[locs] <- 1
               taken[i] <- 0
            }
        }
      }
      m.2 <- m.2[taken == 0,]
     dim(m.2)
   }

   MUT.count <- AMP.count <- DEL.count <- 0
   for (i in 1:nrow(m.2)) {
      temp <- strsplit(row.names(m.2)[i], split="_")
      temp <- strsplit(temp[[1]][length(temp[[1]])], split=" ")      
      suffix <- temp[[1]][1]
      if (!is.na(suffix)) {
         if (suffix == "MUT") MUT.count <- MUT.count + 1
         if (suffix == "AMP") AMP.count <- AMP.count + 1
         if (suffix == "DEL") DEL.count <- DEL.count + 1
     }
   }
   print(paste("Number of features (after filtering and consolidation)",
   nrow(m.2), " MUT:", MUT.count, " AMP:", AMP.count, " DEL:", DEL.count))
   
   # Add location info

   if (!is.null(locs.table.file)) {
      gene.symbol <- row.names(m.2)
      chr <- rep(" ", length(gene.symbol))
      for (i in 1:length(gene.symbol)) {
        temp1 <- strsplit(gene.symbol[i], split="_")
        temp2 <- strsplit(temp1[[1]][1], split="\\.")
        gene.symbol[i] <- ifelse(temp2[[1]][1] == "", temp1[[1]][1], temp2[[1]][1])
        loc <- match(gene.symbol[i], locs.table[,"Approved.Symbol"])
        chr[i] <- ifelse(!is.na(loc), locs.table[loc, "Chromosome"], " ")
       }
      row.names(m.2)  <- paste(row.names(m.2), " ", chr, " ", sep="")
      print(paste("Total unmatched to chromosomal locations:", sum(chr == " "), "out of ", nrow(m.2), "features"))
    }

   # Save filtered and consolidated file

    if (!is.null(save_preprocessed_features_dataset)) {
       write.gct.2(gct.data.frame = data.frame(m.2), descs = row.names(m.2), filename = save_preprocessed_features_dataset)
   }
   
   # Compute MI and % explained with original seed(s)
   
   median_target <- median(target)
    if (target.match == "negative") {
      target.locs <- seq(1, length(target))[target <= median_target]
    } else {
      target.locs <- seq(1, length(target))[target > median_target]
    }

   cmi.orig.seed <- cmi.orig.seed.cum <- pct_explained.orig.seed <- pct_explained.orig.seed.cum <- vector(length=length(seed.names), mode="numeric")
   if (length(seed.names) > 1) {
      seed.cum <- NULL
      for (i in 1:nrow(seed.vectors)) {
         y <- seed.vectors[i,]
         cmi.orig.seed[i] <- assoc(target, y, assoc.metric)
         pct_explained.orig.seed[i] <- sum(y[target.locs])/length(target.locs)
         seed.cum <- apply(rbind(seed.vectors[i,], seed.cum), MARGIN=2, FUN=seed.combination.op)
         cmi.orig.seed.cum[i] <- assoc(target, seed.cum, assoc.metric)
         pct_explained.orig.seed.cum[i] <- sum(seed.cum[target.locs])/length(target.locs)
      }
   } else {
       y <- as.vector(seed.vectors)
       seed.cum <- y
       cmi.orig.seed <- cmi.orig.seed.cum <- assoc(target, y, assoc.metric)
       pct_explained.orig.seed <- sum(y[target.locs])/length(target.locs)
   }
    cmi.seed.iter0 <- assoc(target, seed, assoc.metric)
    pct_explained.seed.iter0 <- sum(seed[target.locs])/length(target.locs) 

   # CMI iterations

   cmi <- pct_explained <- cmi.names <- matrix(0, nrow=nrow(m.2), ncol=max.n.iter)
   cmi.seed <- pct_explained.seed <- vector(length=max.n.iter, mode="numeric")
   seed.names.iter <- vector(length=max.n.iter, mode="character")
   seed.initial <- seed
   seed.iter <- matrix(0, nrow=max.n.iter, ncol=ncol(m.2))

   target.rand <- matrix(target, nrow=n.perm, ncol=ncol(m.2), byrow=TRUE)
   for (i in 1:n.perm) target.rand[i,] <- sample(target.rand[i,])

   for (iter in 1:max.n.iter) {

      cmi.rand <- matrix(0, nrow=nrow(m.2), ncol=n.perm)     
      for (k in 1:nrow(m.2)) {
         if (k %% 100 == 0) print(paste("Iter:", iter, " feature #", k, " out of ", nrow(m.2)))
         y <- m.2[k,]
         cmi[k, iter] <- cond.assoc(target, y, seed, assoc.metric)
         for (j in 1:n.perm) {
            cmi.rand[k, j] <- cond.assoc(target.rand[j,], y, seed, assoc.metric)
          }
       }

      if (target.match == "negative") {
         ind <- order(cmi[, iter], decreasing=F)
      } else {
         ind <- order(cmi[, iter], decreasing=T)
      }
      cmi[, iter] <- cmi[ind, iter]
      cmi.names[, iter] <- row.names(m.2)[ind]
      pct_explained[iter] <- sum(m.2[cmi.names[1, iter], target.locs])/length(target.locs)
      
      # Estimate p-vals and FDRs

      p.val <- FDR <- FDR.lower <- rep(0, nrow(m.2))
      for (i in 1:nrow(m.2)) p.val[i] <- sum(cmi.rand >  cmi[i, iter])/(nrow(m.2)*n.perm)
      FDR <- p.adjust(p.val, method = "fdr", n = length(p.val))
      FDR.lower <- p.adjust(1 - p.val, method = "fdr", n = length(p.val))
    
      for (i in 1:nrow(m.2)) {
         if (cmi[i, iter] < 0) {
            p.val[i] <- 1 - p.val[i]
            FDR[i] <- FDR.lower[i]
         }
         p.val[i] <- signif(p.val[i], 2)
         FDR[i] <- signif(FDR[i], 2)
      }
      p.zero.val <- paste("<", signif(1/(nrow(m.2)*n.perm), 2), sep="")
      p.val <- ifelse(p.val == 0, rep(p.zero.val, length(p.val)), p.val)

      # Make a heatmap of the n.marker top hits in this iteration

      size.mid.panel <- length(seed.names) + iter
      pad.space <- 15
      
      nf <- layout(matrix(c(1, 2, 3, 4), 4, 1, byrow=T), 1, c(2, size.mid.panel, ceiling(n.markers/2) + 4, pad.space), FALSE)
      cutoff <- 2.5
      x <- as.numeric(target)         
      x <- (x - mean(x))/sd(x)         
      ind1 <- which(x > cutoff)
      ind2 <- which(x < -cutoff)
      x[ind1] <- cutoff
      x[ind2] <- -cutoff
      V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      par(mar = c(1, 22, 2, 12))
      image(1:length(target), 1:1, as.matrix(V1), col=mycol, zlim=c(0, ncolors), axes=FALSE,
            main=paste("REVEALER - Iteration:", iter), sub = "", xlab= "", ylab="",
            font=2, family="")
      axis(2, at=1:1, labels=paste("TARGET: ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
           cex.axis=character.scaling, font.axis=1,
           line=0, font=2, family="")
      axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
      axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")
 
      if (iter == 1) {
            V0 <- rbind(seed.vectors, seed + 2)
            cmi.vals <- c(cmi.orig.seed, cmi.seed.iter0)
            cmi.vals <- signif(cmi.vals, 2)
            cmi.cols <- rep("black", length(cmi.orig.seed) + 1)                     # IC/CIC colors
            row.names(V0) <- c(paste("SEED: ", seed.names), "SUMMARY SEED:")
            V0 <- apply(V0, MARGIN=2, FUN=rev)
       } else {
         V0 <- rbind(seed.vectors, m.2[seed.names.iter[1:(iter-1)],], seed + 2)
         row.names(V0) <- c(paste("SEED:   ", seed.names), paste("ITERATION ", seq(1, iter-1), ":  ",
                                                                 seed.names.iter[1:(iter-1)], sep=""), "SUMMARY SEED:")
         cmi.vals <- c(cmi.orig.seed, cmi[1, 1:iter-1], cmi.seed[iter-1])
         cmi.vals <- signif(cmi.vals, 2)
         cmi.cols <- c(rep("black", length(cmi.orig.seed)), rep("steelblue", iter - 1), "black")      # IC/CIC colors
         pct.vals <- c(signif(pct_explained.orig.seed, 2), signif(pct_explained[seq(1, iter - 1)], 2),
                       signif(pct_explained.seed[iter - 1], 2))         
         V0 <- apply(V0, MARGIN=2, FUN=rev)
       }

      all.vals <- cmi.vals
      par(mar = c(1, 22, 0, 12))
      if (feature.type == "discrete") {
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3), col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9],
                                                                    brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5]),
               axes=FALSE, main="", sub = "", xlab= "", ylab="")
      } else { # continuous
         for (i in 1:length(V0[,1])) {
            x <- as.numeric(V0[i,])
            V0[i,] <- (x - mean(x))/sd(x)
            max.v <- max(max(V0[i,]), -min(V0[i,]))
            V0[i,] <- ceiling(ncolors * (V0[i,] - (- max.v))/(1.001*(max.v - (- max.v))))
         }
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="", sub = "",
               xlab= "", ylab="")
      }
#      axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
#      axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
      for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }
      V0 <- m.2[cmi.names[1:n.markers, iter],]
      V0 <- apply(V0, MARGIN=2, FUN=rev)
      par(mar = c(6, 22, 3, 12))
      if (feature.type == "discrete") {
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 1),
               col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9]),
               axes=FALSE, main=paste("Top", n.markers, "Matches"), sub = "", xlab= "", ylab="")
      } else { # continuous
         for (i in 1:length(V0[,1])) {
            cutoff <- 2.5
            x <- as.numeric(V0[i,])            
            x <- (x - mean(x))/sd(x)         
            ind1 <- which(x > cutoff)
            ind2 <- which(x < -cutoff)
            x[ind1] <- cutoff
            x[ind2] <- -cutoff
            V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
         }
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="", sub = "",
               xlab= "", ylab="")
      }
      axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=0.9*character.scaling,
           line=0, font=2, family="")

      all.vals <- paste(signif(cmi[1:n.markers, iter], 2), p.val[1:n.markers], FDR[1:n.markers], sep="   ")

      axis(4, at=nrow(V0)+0.4, labels=" CIC    p-val   FDR", adj= 0.5, tick=FALSE, las = 1,
           cex.axis=0.8*character.scaling, line=0, font=2, family="")
      axis(4, at=c(seq(1, nrow(V0) - 1), nrow(V0) - 0.2), labels=rev(all.vals), adj= 0.5, tick=FALSE,
           las = 1, cex.axis=0.9*character.scaling, line=0, font=2, family="")
      axis(1, at=1:ncol(V0), labels=colnames(V0), adj= 0.5, tick=FALSE,las = 3, cex=1,
           cex.axis=0.45*character.scaling,  line=0, font=2, family="")

     # second page shows the same markers clustered in groups with similar profiles

     tab <- m.2[cmi.names[1:n.markers, iter],]
     all.vals <- paste(signif(cmi[1:n.markers, iter], 2), p.val[1:n.markers], FDR[1:n.markers], sep="   ")

     # Cluster and make heatmap of n.markers top hits in groups

     tab2 <- tab + 0.001

     k.min <- 2
     k.max <- 10
     NMF.models <- nmf(tab2, seq(k.min, k.max), nrun = 25, method="brunet", seed=9876)
     plot(NMF.models)
     NMF.sum <- summary(NMF.models)

     k.vec <- seq(k.min, k.max, 1)
     cophen <- NMF.sum[, "cophenetic"]

     peak <- c(0, rep(0, k.max-2), 0)
     for (h in 2:(length(cophen) - 1)) if (cophen[h - 1] < cophen[h] & cophen[h] > cophen[h + 1]) peak[h] <- 1

     if (sum(peak) == 0) {
        if (cophen[1] > cophen[length(cophen)]) {
           k <- k.min
         } else {
           k <- k.max
         }
     } else {
        k.peaks <- k.vec[peak == 1]
        k <- rev(k.peaks)[1]
     }
     print(paste("Number of groups:", k))
     NMF.model <- nmf(tab2, k, method="brunet", seed=9876)
     classes <- predict(NMF.model, "rows")
     table(classes)
     lens <- table(classes)

     lens2 <- ifelse(lens <= 5, 5, lens)
     lens2[length(lens2)] <- lens2[length(lens2)] + 5


     def.par <- par(no.readonly = TRUE)       
     nf <- layout(matrix(seq(1, k+3), k+3, 1, byrow=T), 1, c(3.5, size.mid.panel, lens2, pad.space), FALSE)      

      cutoff <- 2.5
      x <- as.numeric(target)         
      x <- (x - mean(x))/sd(x)         
      ind1 <- which(x > cutoff)
      ind2 <- which(x < -cutoff)
      x[ind1] <- cutoff
      x[ind2] <- -cutoff
      V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      par(mar = c(1, 22, 1, 12))
      image(1:length(target), 1:1, as.matrix(V1), col=mycol, zlim=c(0, ncolors), axes=FALSE,
            main=paste("REVEALER - Iteration:", iter), sub = "", xlab= "", ylab="",
            font=2, family="")
     axis(2, at=1:1, labels=paste("TARGET: ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
          cex.axis=character.scaling, font.axis=1,
           line=0, font=2, family="")

      axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
      axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")

#      axis(4, at=1:1, labels="  IC/CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=0.8*character.scaling,
#           font.axis=1, line=0, font=2, family="")
 
      if (iter == 1) {
            V0 <- rbind(seed.vectors, seed + 2)
            cmi.vals <- c(cmi.orig.seed, cmi.seed.iter0)
            cmi.vals <- signif(cmi.vals, 2)
            row.names(V0) <- c(paste("SEED: ", seed.names), "SUMMARY SEED:")
            V0 <- apply(V0, MARGIN=2, FUN=rev)
       } else {
         V0 <- rbind(seed.vectors, m.2[seed.names.iter[1:(iter-1)],], seed + 2)
         row.names(V0) <- c(paste("SEED:   ", seed.names), paste("ITERATION ", seq(1, iter-1), ":  ",
                                        seed.names.iter[1:(iter-1)], sep=""), "SUMMARY SEED:")
         cmi.vals <- c(cmi.orig.seed, cmi[1, 1:iter-1], cmi.seed[iter-1])
         cmi.vals <- signif(cmi.vals, 2)
         pct.vals <- c(signif(pct_explained.orig.seed, 2), signif(pct_explained[seq(1, iter - 1)], 2),
                       signif(pct_explained.seed[iter - 1], 2))         
         V0 <- apply(V0, MARGIN=2, FUN=rev)
       }

      all.vals <- cmi.vals
      par(mar = c(1, 22, 0, 12))
      if (feature.type == "discrete") {
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3), col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9],
                                                                    brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5]),
               axes=FALSE, main="", sub = "", xlab= "", ylab="")
      } else { # continuous
         for (i in 1:length(V0[,1])) {
            x <- as.numeric(V0[i,])
            V0[i,] <- (x - mean(x))/sd(x)
            max.v <- max(max(V0[i,]), -min(V0[i,]))
            V0[i,] <- ceiling(ncolors * (V0[i,] - (- max.v))/(1.001*(max.v - (- max.v))))
         }
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="",
               sub = "", xlab= "", ylab="")
      }
      
#      axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
#      axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
      for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }

    # Groups of abnormalities
      
     all.vals <- paste(signif(cmi[1:n.markers, iter], 2), p.val[1:n.markers], FDR[1:n.markers], sep="   ")
      
     for (h in sort(unique(classes))) {      
         if (lens[h] == 1) {
            V0 <- t(as.matrix(tab[classes == h,]))
          } else {
            V0 <- tab[classes == h,]
            V0 <- apply(V0, MARGIN=2, FUN=rev)
          }
         r.names <- row.names(tab)[classes == h]
         all.vals0 <- all.vals[classes == h]         
         if (h < k) {
            par(mar = c(0.5, 22, 1, 12))
          } else {
            par(mar = c(3, 22, 1, 12))
          }
         if (feature.type == "discrete") {
            if (lens[h] == 1) {           
               image(1:ncol(V0), 1, t(V0), zlim = c(0, 1), col=c(brewer.pal(9, "Blues")[3],
                                                               brewer.pal(9, "Blues")[9]),
                     axes=FALSE, main=paste("Top Matches. Group:", h, "(iter ", iter, ")"), sub = "",
                     xlab= "", ylab="", cex.main=0.8)
             } else {
               image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 1), col=c(brewer.pal(9, "Blues")[3],
                                                                        brewer.pal(9, "Blues")[9]),
                     axes=FALSE, main=paste("Top Matches. Group:", h, "(iter ", iter, ")"),
                     sub = "", xlab= "", ylab="", cex.main=0.8)
             }
         } else { # continuous
            for (i in 1:length(V0[,1])) {
               cutoff <- 2.5
               x <- as.numeric(V0[i,])            
               x <- (x - mean(x))/sd(x)         
               ind1 <- which(x > cutoff)
               ind2 <- which(x < -cutoff)
               x[ind1] <- cutoff
               x[ind2] <- -cutoff
               V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
            }
            image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE,
               main=paste("Top Matches. Group:", h), sub = "", xlab= "", ylab="")
         }

         if (lens[h] == 1) {
           axis(2, at=1, labels=rev(r.names), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
                line=-0.7, font=2, family="")
           axis(4, at=1+0.4, labels=" CIC     p-val     FDR", adj= 0.5, tick=FALSE, las = 1,
                cex.axis=0.8*character.scaling, line=-0.7, font=2, family="")
           axis(4, at=1, labels=all.vals0, adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
                line=-0.7, font=2, family="")
         } else {
            axis(2, at=1:nrow(V0), labels=rev(r.names), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
                 line=-0.7, font=2, family="")
            axis(4, at=nrow(V0)+0.4, labels=" CIC     p-val     FDR", adj= 0.5, tick=FALSE, las = 1,
                 cex.axis=0.8*character.scaling, line=-0.7, font=2, family="")            
            axis(4, at=c(seq(1, nrow(V0) - 1), nrow(V0) - 0.2), labels=rev(all.vals0), adj= 0.5,
                 tick=FALSE, las = 1, cex.axis=character.scaling, line=-0.7, font=2, family="")
         }
      }
      par(def.par)
      
      # Update seed

      seed.names.iter[iter] <- cmi.names[1, iter] # top hit from this iteration
      seed <- apply(rbind(seed, m.2[seed.names.iter[iter],]), MARGIN=2, FUN=seed.combination.op)
      seed.iter[iter,] <- seed
      cmi.seed[iter] <- assoc(target, seed, assoc.metric)
      pct_explained.seed[iter] <- sum(seed[target.locs])/length(target.locs)
      
    } # end of iterations loop

   # Final summary figures -----------------------------------------------------------------------------

   summ.panel <- length(seed.names) + 2 * max.n.iter + 2
  
   legend.size <- 4
   pad.space <- 30 - summ.panel - legend.size

   nf <- layout(matrix(c(1, 2, 3, 0), 4, 1, byrow=T), 1, c(2, summ.panel, legend.size, pad.space), FALSE)

   cutoff <- 2.5
   x <- as.numeric(target)         
   x <- (x - mean(x))/sd(x)         
   ind1 <- which(x > cutoff)
   ind2 <- which(x < -cutoff)
   x[ind1] <- cutoff
   x[ind2] <- -cutoff
   V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))

   par(mar = c(1, 22, 2, 12))
   image(1:length(target), 1:1, as.matrix(V1), zlim=c(0, ncolors), col=mycol, axes=FALSE,
         main=paste("REVEALER - Results"), sub = "", xlab= "", ylab="", font=2, family="")
  
   axis(2, at=1:1, labels=paste("TARGET:  ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
        cex.axis=character.scaling,  line=0, font=2, family="")

   axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
   axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")

#   axis(4, at=1:1, labels="  IC   ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")

   V0 <- rbind(seed.vectors + 2, seed.cum) 
   for (i in 1:max.n.iter) {
      V0 <- rbind(V0,
                  m.2[seed.names.iter[i],] + 2,
                  seed.iter[i,])
   }

   row.names.V0 <- c(paste("SEED:   ", seed.names), "SUMMARY SEED:")
   for (i in 1:max.n.iter) {
      row.names.V0 <- c(row.names.V0, paste("ITERATION ", i, ":  ", seed.names.iter[i], sep=""),
                        paste("SUMMARY FEATURE ", i, ":  ", sep=""))
   }
   row.names(V0) <- row.names.V0

   cmi.vals <- c(cmi.orig.seed, cmi.orig.seed.cum[length(seed.names)])                 
   for (i in 1:max.n.iter) {
      cmi.vals <- c(cmi.vals, as.vector(cmi[1, i]), cmi.seed[i])
    }
   cmi.vals <- signif(cmi.vals, 2)
   all.vals <-cmi.vals

   cmi.cols <- c(rep("black", length(cmi.orig.seed)), "black", rep(c("steelblue", "black"), max.n.iter))                     # IC/CIC colors   

   V0 <- apply(V0, MARGIN=2, FUN=rev)

   par(mar = c(7, 22, 0, 12))
   if (feature.type == "discrete") {  
       image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3),
       col=c(brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5],                          
             brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9]), axes=FALSE, main="",
             sub = "", xlab= "", ylab="")
   } else { # continuous
      for (i in 1:nrow(V0)) {
         cutoff <- 2.5
         x <- as.numeric(V0[i,])
         x <- (x - mean(x))/sd(x)         
         ind1 <- which(x > cutoff)
         ind2 <- which(x < -cutoff)
         x[ind1] <- cutoff
         x[ind2] <- -cutoff
         V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      }
      image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="",
            sub = "", xlab= "", ylab="")
   }
#   axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
#   axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
    for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }
   
   axis(1, at=1:ncol(V0), labels=colnames(V0), adj= 0.5, tick=FALSE,las = 3, cex=1, cex.axis=0.4*character.scaling,
        line=0, font=2, family="")

        # Legend

      par.mar <- par("mar")
      par(mar = c(3, 35, 8, 10))
      leg.set <- seq(-cutoff, cutoff, 2*cutoff/100)
      image(1:101, 1:1, as.matrix(leg.set), zlim=c(-cutoff, cutoff), col=mycol, axes=FALSE, main="",
          sub = "", xlab= "", ylab="",font=2, family="")
      ticks <- c(-2, -1, 0, 1, 2)
      tick.cols <- rep("black", 5)
      tick.lwd <- c(1,1,2,1,1)
      locs <- NULL
      for (k in 1:length(ticks)) locs <- c(locs, which.min(abs(ticks[k] - leg.set)))
      axis(1, at=locs, labels=ticks, adj= 0.5, tick=T, cex=0.8, cex.axis=1, line=0, font=2, family="")
      mtext("Standardized Target Profile", cex=0.8, side = 1, line = 3.5, outer=F)
      par(mar = par.mar)

   V0 <- rbind(target, seed.vectors, seed.cum) 
   for (i in 1:max.n.iter) {
      V0 <- rbind(V0,
                  m.2[seed.names.iter[i],],
                  seed.iter[i,])
   }
   V0.colnames <- colnames(V0)
   V0 <- cbind(V0, c(1, all.vals))
   colnames(V0) <- c(V0.colnames, "IC")

   row.names.V0 <- c(target.name, seed.names, "SUMMARY SEED:")
   for (i in 1:max.n.iter) {
      row.names.V0 <- c(row.names.V0, seed.names.iter[i], paste("SUMMARY FEATURE ", i, ":  ", sep=""))
   }
   row.names(V0) <- row.names.V0
  
  # Version without summaries ----------------------------------------------------

   summ.panel <- length(seed.names) + max.n.iter + 2
  
   legend.size <- 4
   pad.space <- 30 - summ.panel - legend.size
   
   nf <- layout(matrix(c(1, 2, 3, 0), 4, 1, byrow=T), 1, c(2, summ.panel, legend.size, pad.space), FALSE)

   cutoff <- 2.5
   x <- as.numeric(target)         
   x <- (x - mean(x))/sd(x)         
   ind1 <- which(x > cutoff)
   ind2 <- which(x < -cutoff)
   x[ind1] <- cutoff
   x[ind2] <- -cutoff
   V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))

   par(mar = c(1, 22, 2, 12))
   image(1:length(target), 1:1, as.matrix(V1), zlim=c(0, ncolors), col=mycol, axes=FALSE,
         main=paste("REVEALER - Results"),
         sub = "", xlab= "", ylab="", font=2, family="")
  
   axis(2, at=1:1, labels=paste("TARGET:  ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
        cex.axis=character.scaling,  line=0, font=2, family="")
   
#   axis(4, at=1:1, labels="  IC   ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")

      axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
      axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")

   
   V0 <- seed.vectors + 2
   for (i in 1:max.n.iter) {
      V0 <- rbind(V0,
                  m.2[seed.names.iter[i],] + 2)
   }
   V0 <- rbind(V0, seed.iter[max.n.iter,])

   row.names.V0 <- c(paste("SEED:   ", seed.names))
   for (i in 1:max.n.iter) {
      row.names.V0 <- c(row.names.V0, paste("ITERATION ", i, ":  ", seed.names.iter[i], sep=""))
   }
   row.names(V0) <- c(row.names.V0, "FINAL SUMMARY")

   cmi.vals <- cmi.orig.seed
   for (i in 1:max.n.iter) {
      cmi.vals <- c(cmi.vals, as.vector(cmi[1, i]))
    }
   cmi.vals <- c(cmi.vals, cmi.seed[max.n.iter])
   cmi.vals <- signif(cmi.vals, 2)
   all.vals <-cmi.vals

   cmi.cols <- c(rep("black", length(cmi.orig.seed)), rep("steelblue", max.n.iter), "black")    # IC/CIC colors   
      
   V0 <- apply(V0, MARGIN=2, FUN=rev)
   par(mar = c(7, 22, 0, 12))   
   
   if (feature.type == "discrete") {  
       image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3),
       col=c(brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5],                          
              brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9]), axes=FALSE, main="",
             sub = "", xlab= "", ylab="")
   } else { # continuous
      for (i in 1:nrow(V0)) {
         cutoff <- 2.5
         x <- as.numeric(V0[i,])
         x <- (x - mean(x))/sd(x)         
         ind1 <- which(x > cutoff)
         ind2 <- which(x < -cutoff)
         x[ind1] <- cutoff
         x[ind2] <- -cutoff
         V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      }
      image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="",
            sub = "", xlab= "", ylab="")
   }
#   axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
#   axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
      for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }
   
   axis(1, at=1:ncol(V0), labels=colnames(V0), adj= 0.5, tick=FALSE,las = 3, cex=1, cex.axis=0.4*character.scaling,
        line=0, font=2, family="")

        # Legend

      par.mar <- par("mar")
      par(mar = c(3, 35, 8, 10))
      leg.set <- seq(-cutoff, cutoff, 2*cutoff/100)
      image(1:101, 1:1, as.matrix(leg.set), zlim=c(-cutoff, cutoff), col=mycol, axes=FALSE, main="",
          sub = "", xlab= "", ylab="",font=2, family="")
      ticks <- c(-2, -1, 0, 1, 2)
      tick.cols <- rep("black", 5)
      tick.lwd <- c(1,1,2,1,1)
      locs <- NULL
      for (k in 1:length(ticks)) locs <- c(locs, which.min(abs(ticks[k] - leg.set)))
      axis(1, at=locs, labels=ticks, adj= 0.5, tick=T, cex=0.8, cex.axis=0.8, line=0, font=2, family="")
      mtext("Standardized Target Profile", cex=0.8, side = 1, line = 3.5, outer=F)
      par(mar = par.mar)

  # Landscape plot ---------------------------------------------------------------

#  if (produce.lanscape.plot == T) {
#  
#   nf <- layout(matrix(c(1, 2), 2, 1, byrow=T), 1, c(2, 1), FALSE)
#
#    if (length(as.vector(cmi.names[1:top.n, 1:max.n])) > 1) {
#       V0 <- rbind(seed.vectors, as.matrix(m.2[as.vector(cmi.names[1:top.n, 1:max.n]),]))
#    } else {
#       V0 <- rbind(seed.vectors, t(as.matrix(m.2[as.vector(cmi.names[1:top.n, 1:max.n]),])))
#    }
#   
#   number.seq <- NULL
#   for (i in 1:max.n) number.seq <- c(number.seq, rep(i, top.n))
#
#   row.names(V0) <-  c(paste("SEED: ", seed.names, "(", signif(cmi.orig.seed, 2), ")"),
#                       paste("ITER ", number.seq, ":", as.vector(cmi.names[1:top.n, 1:max.n]),
#                             "(", signif(as.vector(cmi[1:top.n, 1:max.n]), 2), ")"))
# 
#    cmi.vals <- c(cmi.orig.seed, as.vector(cmi[1:top.n, 1:max.n]))
#
#   total.points <- row(V0)
#   V2 <- V0
#   metric.matrix <- matrix(0, nrow=nrow(V2), ncol=nrow(V2))
#   row.names(metric.matrix)  <- row.names(V2)
#   colnames(metric.matrix) <- row.names(V2)
#   MI.ref <- cmi.vals
#   for (i in 1:nrow(V2)) {
#      for (j in 1:i) {
#           metric.matrix[i, j] <- assoc (V2[j,], V2[i,], assoc.metric)
#      }
#   }
#   metric.matrix
#   metric.matrix <- metric.matrix + t(metric.matrix)
#   metric.matrix
#   alpha <- 5
#   metric.matrix2 <- 1 - ((1/(1+exp(-alpha*metric.matrix))))
#   for (i in 1:nrow(metric.matrix2)) metric.matrix2[i, i] <- 0
#   metric.matrix2
# 
#   smacof.map <- smacofSphere(metric.matrix2, ndim = 2, weightmat = NULL, init = NULL,
#                                     ties = "primary", verbose = FALSE, modulus = 1, itmax = 1000, eps = 1e-6)
#   x0 <- smacof.map$conf[,1]
#   y0 <- smacof.map$conf[,2]
#   r <- sqrt(x0*x0 + y0*y0)
#   radius <-  1 - ((1/(1+exp(-alpha*MI.ref))))
#   x <- x0*radius/r
#   y <- y0*radius/r
#   angles <- atan2(y0, x0)
#   
#   par(mar = c(4, 7, 4, 7))
# 
#   plot(x, y, pch=20, bty="n", xaxt='n', axes = FALSE, type="n", xlab="", ylab="",
#        main=paste("REVEALER - Landscape for ", target.name),
#        xlim=1.2*c(-max(radius), max(radius)), ylim=1.2*c(-max(radius), max(radius)))
#   line.angle <- seq(0, 2*pi-0.001, 0.001)
#   for (i in 1:length(x)) {
#      line.max.x <- radius[i] * cos(line.angle)
#      line.max.y <- radius[i] * sin(line.angle)
#      points(line.max.x, line.max.y, type="l", col="gray80", lwd=1)
#      points(c(0, x[i]), c(0, y[i]), type="l", col="gray80", lwd=1)
#   }
#   line.max.x <- 1.2*max(radius) * cos(line.angle)
#   line.max.y <- 1.2*max(radius) * sin(line.angle)
#   points(line.max.x, line.max.y, type="l", col="purple", lwd=2)
#   points(0, 0, pch=21, bg="red", col="black", cex=2.5)   
#   points(x, y, pch=21, bg="steelblue", col="black", cex=2.5)
#
#   x <- c(0, x)
#   y <- c(0, y)
# 
#   text(x[1], y[1], labels=print.names[1], pos=2, cex=0.85, col="red", offset=1, font=2, family="")   
#   for (i in 2:length(x)) {
#      pos <- ifelse(x[i] <= 0.25, 4, 2)
#     text(x[i], y[i], labels=print.names[i], pos=pos, cex=0.50, col="darkblue", offset=1, font=2, family="")   
#    }
#
#  }
   
   dev.off()
}

assoc <- function(x, y, metric) {

# Pairwise association of x and y
                                        
    if (length(unique(x)) == 1 || length(unique(y)) == 1) return(0)
    if (metric == "IC") {
       return(mutual.inf.v2(x = x, y = y, n.grid=25)$IC)
    } else if (metric == "COR") {
        return(cor(x, y))
    }
}

cond.assoc <-  function(x, y, z, metric) { # Association of a and y given z
#
# Conditional mutual information I(x, y | z)
#
    if (length(unique(x)) == 1 || length(unique(y)) == 1) return(0)

    if (length(unique(z)) == 1) {  # e.g. for NULLSEED
       if (metric == "IC") {
          return(mutual.inf.v2(x = x, y = y, n.grid = 25)$IC)
       } else if (metric == "COR") {
          return(cor(x, y))
       }
   } else {
       if (metric == "IC") {
          return(cond.mutual.inf(x = x, y = y, z = z, n.grid = 25)$CIC)
       } else if (metric == "COR") {
          return(pcor.test(x, y, z)$estimate)
       }
   }
}

mutual.inf.v2 <- function(x, y, n.grid=25, delta = c(bcv(x), bcv(y))) {
#
# Computes the Mutual Information/Information Coefficient IC(x, y)
#
   # Compute correlation-dependent bandwidth

   rho <- cor(x, y)
   rho2 <- abs(rho)
   delta <- delta*(1 + (-0.75)*rho2)

   # Kernel-based prob. density
   
   kde2d.xy <- kde2d(x, y, n = n.grid, h = delta)
   FXY <- kde2d.xy$z + .Machine$double.eps
   dx <- kde2d.xy$x[2] - kde2d.xy$x[1]
   dy <- kde2d.xy$y[2] - kde2d.xy$y[1]
   PXY <- FXY/(sum(FXY)*dx*dy)
   PX <- rowSums(PXY)*dy
   PY <- colSums(PXY)*dx
   HXY <- -sum(PXY * log(PXY))*dx*dy
   HX <- -sum(PX * log(PX))*dx
   HY <- -sum(PY * log(PY))*dy

   PX <- matrix(PX, nrow=n.grid, ncol=n.grid)
   PY <- matrix(PY, byrow = TRUE, nrow=n.grid, ncol=n.grid)

   MI <- sum(PXY * log(PXY/(PX*PY)))*dx*dy
   rho <- cor(x, y)
   SMI <- sign(rho) * MI

   # Use peason correlation the get the sign (directionality)   
   
   IC <- sign(rho) * sqrt(1 - exp(- 2 * MI)) 
   
   NMI <- sign(rho) * ((HX + HY)/HXY - 1)  

   return(list(MI=MI, SMI=SMI, HXY=HXY, HX=HX, HY=HY, NMI=NMI, IC=IC))
}


cond.mutual.inf <- function(x, y, z, n.grid=25, delta = 0.25*c(bcv(x), bcv(y), bcv(z))) {
 # Computes the Conditional mutual imnformation: 
 # I(X, Y | X) = H(X, Z) + H(Y, Z) - H(X, Y, Z) - H(Z)
 # The 0.25 in front of the bandwidth is because different conventions between bcv and kde3d

   # Compute correlation-dependent bandwidth
    
   rho <- cor(x, y)
   rho2 <- ifelse(rho < 0, 0, rho)
   delta <- delta*(1 + (-0.75)*rho2)

   # Kernel-based prob. density
   
   kde3d.xyz <- kde3d(x=x, y=y, z=z, h=delta, n = n.grid)
   X <- kde3d.xyz$x
   Y <- kde3d.xyz$y
   Z <- kde3d.xyz$z
   PXYZ <- kde3d.xyz$d + .Machine$double.eps
   dx <- X[2] - X[1]
   dy <- Y[2] - Y[1]
   dz <- Z[2] - Z[1]

   # Normalize density and calculate marginal densities and entropies
   
   PXYZ <- PXYZ/(sum(PXYZ)*dx*dy*dz)
   PXZ <- colSums(aperm(PXYZ, c(2,1,3)))*dy
   PYZ <- colSums(PXYZ)*dx
   PZ <- rowSums(aperm(PXYZ, c(3,1,2)))*dx*dy
   PXY <- colSums(aperm(PXYZ, c(3,1,2)))*dz
   PX <- rowSums(PXYZ)*dy*dz
   PY <- rowSums(aperm(PXYZ, c(2,1,3)))*dx*dz
   
   HXYZ <- - sum(PXYZ * log(PXYZ))*dx*dy*dz
   HXZ <- - sum(PXZ * log(PXZ))*dx*dz
   HYZ <- - sum(PYZ * log(PYZ))*dy*dz
   HZ <-  - sum(PZ * log(PZ))*dz
   HXY <- - sum(PXY * log(PXY))*dx*dy   
   HX <-  - sum(PX * log(PX))*dx
   HY <-  - sum(PY * log(PY))*dy

   MI <- HX + HY - HXY   
   CMI <- HXZ + HYZ - HXYZ - HZ

   SMI <- sign(rho) * MI
   SCMI <- sign(rho) * CMI

   IC <- sign(rho) * sqrt(1 - exp(- 2 * MI))
   CIC <- sign(rho) * sqrt(1 - exp(- 2 * CMI))
   
   return(list(CMI=CMI, MI=MI, SCMI=SCMI, SMI=SMI, HXY=HXY, HXYZ=HXYZ, IC=IC, CIC=CIC))
 }

MSIG.Gct2Frame <- function(filename = "NULL") { 
#
# Read a gene expression dataset in GCT format and converts it into an R data frame
#
   ds <- read.delim(filename, header=T, sep="\t", skip=2, row.names=1, blank.lines.skip=T, comment.char="", as.is=T, na.strings = "")
   descs <- ds[,1]
   ds <- ds[-1]
   row.names <- row.names(ds)
   names <- names(ds)
   return(list(ds = ds, row.names = row.names, descs = descs, names = names))
}

write.gct.2 <- function(gct.data.frame, descs = "", filename)
#
# Write output GCT file
#
{
    f <- file(filename, "w")
    cat("#1.2", "\n", file = f, append = TRUE, sep = "")
    cat(dim(gct.data.frame)[1], "\t", dim(gct.data.frame)[2], "\n", file = f, append = TRUE, sep = "")
    cat("Name", "\t", file = f, append = TRUE, sep = "")
    cat("Description", file = f, append = TRUE, sep = "")

    colnames <- colnames(gct.data.frame)
    cat("\t", colnames[1], file = f, append = TRUE, sep = "")

    if (length(colnames) > 1) {
       for (j in 2:length(colnames)) {
           cat("\t", colnames[j], file = f, append = TRUE, sep = "")
       }
     }
    cat("\n", file = f, append = TRUE, sep = "\t")

    oldWarn <- options(warn = -1)
    m <- matrix(nrow = dim(gct.data.frame)[1], ncol = dim(gct.data.frame)[2] +  2)
    m[, 1] <- row.names(gct.data.frame)
    if (length(descs) > 1) {
        m[, 2] <- descs
    } else {
        m[, 2] <- row.names(gct.data.frame)
    }
    index <- 3
    for (i in 1:dim(gct.data.frame)[2]) {
        m[, index] <- gct.data.frame[, i]
        index <- index + 1
    }
    write.table(m, file = f, append = TRUE, quote = FALSE, sep = "\t", eol = "\n", col.names = FALSE, row.names = FALSE)
    close(f)
    options(warn = 0)

}

  SE_assoc <- function(x, y) {
           return(2 - sqrt(mean((x - y)^2)))
       }

REVEALER_assess_features.v1 <- function(
      input_dataset,
      target,
      direction              = "positive",
      feature.files,                           
      features,                                
      output.file,
      description            = "",
      sort.by.target         = T,
      character.scaling      = 1.5,
      n.perm                 = 10000,
      create.feature.summary = F,
      feature.combination.op = "max")
 {
   suppressPackageStartupMessages(library(maptools))
   suppressPackageStartupMessages(library(RColorBrewer))

   set.seed(5209761)

   missing.value.color <- "khaki1"
   mycol <- vector(length=512, mode = "numeric")
   for (k in 1:256) mycol[k] <- rgb(255, k - 1, k - 1, maxColorValue=255)
   for (k in 257:512) mycol[k] <- rgb(511 - (k - 1), 511 - (k - 1), 255, maxColorValue=255)
   mycol <- rev(mycol)
   max.cont.color <- 512
   mycol <- c(mycol,
              missing.value.color)                  # Missing feature color

   categ.col <- c("#9DDDD6", # dusty green
                     "#F0A5AB", # dusty red
                     "#9AC7EF", # sky blue
                     "#F970F9", # violet
                     "#FFE1DC", # clay
                     "#FAF2BE", # dusty yellow
                     "#AED4ED", # steel blue
                     "#C6FA60", # green
                     "#D6A3FC", # purple
                     "#FC8962", # red
                     "#F6E370", # orange
                     "#F0F442", # yellow
                     "#F3C7F2", # pink
                     "#D9D9D9", # grey
                     "#FD9B85", # coral
                     "#7FFF00", # chartreuse
                     "#FFB90F", # goldenrod1
                     "#6E8B3D", # darkolivegreen4
                     "#8B8878", # cornsilk4
                     "#7FFFD4") # aquamarine

   cex.size.table <- c(1, 1, 1, 1, 1, 1, 1, 1, 1, 0.9,   # 1-10 characters
                       0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, # 11-20 characters
                       0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8)

   pdf(file=output.file, height=14, width=11)


   n.panels <- length(feature.files) 
   l.panels <- NULL
   for (l in 1:n.panels) l.panels <- c(l.panels, 1.5, length(features[[l]]))
   l.panels[l.panels < 2] <- 1.5
   empty.panel <- 30 - sum(unlist(l.panels))
   l.panels <- c(l.panels, empty.panel)
   n.panels <- length(l.panels)

   nf <- layout(matrix(c(seq(1, n.panels - 1), 0), n.panels, 1, byrow=T), 1, l.panels,  FALSE)
                      
   for (f in 1:length(feature.files)) {   # loop over feature types
      dataset <- MSIG.Gct2Frame(filename = input_dataset)
      m.1 <- data.matrix(dataset$ds)
      sample.names.1 <- colnames(m.1)
      Ns.1 <- ncol(m.1)

      target.vec <- m.1[target,]

      non.nas <- !is.na(target.vec)

      target.vec <- target.vec[non.nas]
      sample.names.1 <- sample.names.1[non.nas]
      Ns.1 <- length(target.vec)
      m.1 <- m.1[, non.nas]

      dataset.2 <- MSIG.Gct2Frame(filename = feature.files[[f]])
      m.2 <- data.matrix(dataset.2$ds)
      dim(m.2)
      row.names(m.2) <- dataset.2$row.names
      Ns.2 <- ncol(m.2)  
      sample.names.2 <- colnames(m.2) <- dataset.2$names
      
      overlap <- intersect(sample.names.1, sample.names.2)
      locs1 <- match(overlap, sample.names.1)
      locs2 <- match(overlap, sample.names.2)
      m.1 <- m.1[, locs1]
      target.vec <- target.vec[locs1]
      m.2 <- m.2[, locs2]
      Ns.1 <- ncol(m.1)
      Ns.2 <- ncol(m.2)
      sample.names.1 <- colnames(m.1)
      sample.names.2 <- colnames(m.2)
      
      if (sort.by.target == T) {
         if (direction == "positive") {
            ind <- order(target.vec, decreasing=T)
         } else {
            ind <- order(target.vec, decreasing=F)
         }
         target.vec <- target.vec[ind]
         sample.names.1 <- sample.names.1[ind]
         m.1 <- m.1[, ind]
         m.2 <- m.2[, ind]         
         sample.names.2 <- sample.names.2[ind]
      }

    # normalize target
      target.vec.orig <- target.vec
      unique.target.vals <- unique(target.vec)
      n.vals <- length(unique.target.vals)
      if (n.vals >= length(target.vec)*0.5) {    # Continuous value color map        
         cutoff <- 2.5
         x <- target.vec
         x <- (x - mean(x))/sd(x)         
         x[x > cutoff] <- cutoff
         x[x < - cutoff] <- - cutoff      
         x <- ceiling((max.cont.color - 1) * (x + cutoff)/(cutoff*2)) + 1
         target.vec <- x
      }
      if (f == 1) {
          main <- description
      } else {
          main <- ""
      }
      par(mar = c(0, 22, 2, 9))

      target.nchar <- ifelse(nchar(target) > 30, 30, nchar(target))
      cex.axis <- cex.size.table[target.nchar]
      
      if (n.vals >= length(target.vec)*0.5) {    # Continuous value color map        
          image(1:Ns.1, 1:1, as.matrix(target.vec), zlim = c(0, max.cont.color), col=mycol[1: max.cont.color],
                axes=FALSE, main=main, sub = "", xlab= "", ylab="")
      } else if (n.vals == 2) {  # binary
         image(1:Ns.1, 1:1, as.matrix(target.vec), zlim = range(target.vec), col=brewer.pal(9, "Blues")[3],
               brewer.pal(9, "Blues")[9],
               axes=FALSE, main=main, sub = "", xlab= "", ylab="")
      } else {  # categorical
         image(1:Ns.1, 1:1, as.matrix(target.vec), zlim = range(target.vec), col=categ.col[1:n.vals],
               axes=FALSE, main=main, sub = "", xlab= "", ylab="")
      }
      axis(2, at=1:1, labels=target, adj= 0.5, tick=FALSE, las = 1, cex=1, cex.axis=cex.axis*character.scaling,
           font.axis=1, line=0, font=2, family="")
      axis(4, at=1:1, labels=paste("   IC       p-val"), adj= 0.5, tick=FALSE, las = 1, cex=1,
           cex.axis=0.7*character.scaling,
           font.axis=1, line=0, font=2, family="")

      feature.mat <- feature.names <- NULL
      
     for (feat.n in 1:length(features[[f]])) { 
        len <- length(unlist(features[[f]][feat.n]))         
        feature.name <- unlist(features[[f]][feat.n])

        if (is.na(match(feature.name, row.names(m.2)))) next
        feature.mat <- rbind(feature.mat,  m.2[feature.name,])
        feature.names <- c(feature.names, feature.name)
      }
      feature.mat <- as.matrix(feature.mat)
      row.names(feature.mat) <- feature.names

      if (create.feature.summary == T) {
         summary.feature <- apply(feature.mat, MARGIN=2, FUN=feature.combination.op) + 2
         feature.mat <- rbind(feature.mat, summary.feature)
         row.names(feature.mat) <- c(feature.names, "SUMMARY FEATURE")
     }

      for (i in 1:nrow(feature.mat)) {

           feature.vec <- feature.mat[i,]
           unique.feature.vals <- unique(sort(feature.vec))
           non.NA.vals <- sum(!is.na(feature.vec))
           n.vals <- length(unique.feature.vals)
           if (n.vals > 2) {    # Continuous value color map        
              feature.vals.type <- "continuous"
              cutoff <- 2.5
              x <- feature.vec
              locs.non.na <- !is.na(x)
              x.nonzero <- x[locs.non.na]
              x.nonzero <- (x.nonzero - mean(x.nonzero))/sd(x.nonzero)         
              x.nonzero[x.nonzero > cutoff] <- cutoff
              x.nonzero[x.nonzero < - cutoff] <- - cutoff      
              feature.vec[locs.non.na] <- x.nonzero
              feature.vec2 <- feature.vec
              feature.vec[locs.non.na] <- ceiling((max.cont.color - 2) * (feature.vec[locs.non.na] + cutoff)/(cutoff*2)) + 1
              feature.vec[is.na(x)] <- max.cont.color + 1
              feature.mat[i,] <- feature.vec              
           }
      }
      feature.mat <- as.matrix(feature.mat)

      # compute IC association with target

      IC.vec <- p.val.vec <- stats.vec <- NULL
      sqr_error.vec <- roc.vec <- NULL
      
      for (i in 1:nrow(feature.mat)) {
           feature.vec <- feature.mat[i,]
#           IC <- IC.v1(target.vec, feature.vec)
           IC <- mutual.inf.v2(x = target.vec.orig, y = feature.vec, n.grid=25)$IC

           feature.vec.0.1 <- (feature.vec - min(feature.vec))/(max(feature.vec) - min(feature.vec))           
           target.vec.0.1 <- (target.vec - min(target.vec))/(max(target.vec) - min(target.vec))
           if (direction == "negative") target.vec.0.1 <- 1 - target.vec.0.1
           sqr_error <- SE_assoc(target.vec.0.1, feature.vec.0.1)           
           roc <- roc.area(feature.vec.0.1, target.vec.0.1)$A
           IC <- signif(IC, 3)
           null.IC <- vector(length=n.perm, mode="numeric")

       for (h in 1:n.perm) null.IC[h] <- mutual.inf.v2(x = sample(target.vec.orig), y = feature.vec, n.grid=25)$IC
           if (IC >= 0) {
             p.val <- sum(null.IC >= IC)/n.perm
           } else {
             p.val <- sum(null.IC <= IC)/n.perm
           }
           p.val <- signif(p.val, 3)
           if (p.val == 0) {
             p.val <- paste("<", signif(1/n.perm, 3), sep="")
           }

           IC.vec <- c(IC.vec, IC)
           sqr_error.vec <- c(sqr_error.vec, sqr_error)
           roc.vec <- c(roc.vec, roc)
           p.val.vec <- c(p.val.vec, p.val)
           space.chars <- "           "
           IC.char <- nchar(IC)
           pad.char <- substr(space.chars, 1, 10 - IC.char)
           stats.vec <- c(stats.vec, paste(IC, pad.char, p.val, sep=""))
       }

      if (nrow(feature.mat) > 1) {
          V <- apply(feature.mat, MARGIN=2, FUN=rev)
      } else {
          V <- as.matrix(feature.mat)
      }
      
      features.max.nchar <- max(nchar(row.names(V)))
      features.nchar <- ifelse(features.max.nchar > 30, 30, features.max.nchar)
      cex.axis <- cex.size.table[features.nchar]

      par(mar = c(1, 22, 1, 9))
      
     if (n.vals > 2) {    # Continuous value color map        
         image(1:dim(V)[2], 1:dim(V)[1], t(V), zlim = c(0, max.cont.color + 3), col=mycol, axes=FALSE, main=main,
               cex.main=0.8, sub = "", xlab= "", ylab="")
     } else {  # binary
         image(1:dim(V)[2], 1:dim(V)[1], t(V), zlim = c(0, 3), col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9],
                                                                    brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5]),
               axes=FALSE, main="", cex.main=0.8,  sub = "", xlab= "", ylab="")
     }
     axis(2, at=1:dim(V)[1], labels=row.names(V), adj= 0.5, tick=FALSE, las = 1, cex=1, cex.axis=cex.axis*character.scaling,
           font.axis=1, line=0, font=2, family="")
     axis(4, at=1:dim(V)[1], labels=rev(stats.vec), adj= 0.5, tick=FALSE, las = 1, cex=1, cex.axis=0.7*character.scaling,
           font.axis=1, line=0, font=2, family="")
     }
   dev.off()
   
}

#   list.of.packages <- c("misc3d", "MASS", "smacof", "NMF", "RColorBrewer", "ppcor", "maptools")
#   new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
#   if(length(new.packages)) install.packages(new.packages)

  # Check if all neccessary packages are installed and if not install those missing

   suppressPackageStartupMessages(library(misc3d))
   suppressPackageStartupMessages(library(MASS))
#   suppressPackageStartupMessages(library(smacof))
   suppressPackageStartupMessages(library(NMF))
   suppressPackageStartupMessages(library(RColorBrewer))
   suppressPackageStartupMessages(library(ppcor))
   suppressPackageStartupMessages(library(maptools))

  ### Revealer 2.0  February 6, 2013

  REVEALER.v1 <- function(
   # REVEALER (Repeated Evaluation of VariablEs conditionAL Entropy and Redundancy) is an analysis method specifically suited
   # to find groups of genomic alterations that match in a complementary way, a predefined functional activation, dependency of
   # drug response “target” profile. The method starts by considering, if available, already known genomic alterations (“seed”)
   # that are the known “causes” or are known or assumed “associated” with the target. REVEALER starts by finding the genomic
   # alteration that best matches the target profile “conditional” to the known seed profile using the conditional mutual information.
   # The newly discovered alteration is then merged with the seed to form a new “summary” feature, and then the process repeats itself
   # finding additional complementary alterations that explain more and more of the target profile.

   ds1,                                          # Dataset that contains the "target"
   target.name,                                  # Target feature (row in ds1)
   target.match = "positive",                    # Use "positive" to match the higher values of the target, "negative" to match the lower values
   ds2,                                          # Features dataset.
   seed.names = NULL,                            # Seed(s) name(s)
   exclude.features = NULL,                      # Features to exclude for search iterations
   max.n.iter = 5,                               # Maximun number of iterations
   pdf.output.file,                              # PDF output file
   count.thres.low = NULL,                       # Filter out features with less than count.thres.low 1's
   count.thres.high = NULL,                      # Filter out features with more than count.thres.low 1's

   n.markers = 30,                               # Number of top hits to show in heatmap for every iteration
   locs.table.file = NULL)                       # Table with chromosomal location for each gene symbol (optional)

{  # Additional internal settings
    
   identifier = "REVEALER"                      # Documentation suffix to be added to output file    
   n.perm = 10                                  # Number of permutations (x number of genes) for computing p-vals and FRDs
   save_preprocessed_features_dataset = NULL    # Save preprocessed features dataset    
   seed.combination.op = "max"                  # Operation to consolidate and summarize seeds to one vector of values
   assoc.metric = "IC"                          # Assoc. Metric: "IC" information coeff.; "COR" correlation.
   normalize.features = F                       # Feature row normalization: F or "standardize" or "0.1.rescaling"
   top.n = 1                                    # Number of top hits in each iteration to diplay in Landscape plot
   max.n = 2                                    # Maximum number of iterations to diplay in Landscape plot
   phen.table = NULL                            # Table with phenotypes for each sample (optional)
   phen.column = NULL                           # Column in phen.table containing the relevant phenotype info
   phen.selected = NULL                         # Use only samples of these phenotypes in REVEALER analysis
   produce.lanscape.plot = F                    # Produce multi-dimensional scaling projection plot
   character.scaling = 1.25                     # Character scaling for heatmap
   r.seed = 34578                               # Random number generation seed
   consolidate.identical.features = F           # Consolidate identical features: F or "identical" or "similar" 
   cons.features.hamming.thres = NULL           # If consolidate.identical.features = "similar" then consolidate features within this Hamming dist. thres.

# ------------------------------------------------------------------------------------------------------------------------------
   print(paste("ds1:", ds1))
   print(paste("target.name:", target.name))
   print(paste("target.match:", target.match))
   print(paste("ds2:", ds2))                        
   print(paste("seed.names:", seed.names))
#   print(paste("exclude.features:", exclude.features))
   print(paste("max.n.iter:", max.n.iter))
   print(paste("pdf.output.file:", pdf.output.file))
#   print(paste("n.perm:", n.perm))                  
   print(paste("count.thres.low:", count.thres.low))
   print(paste("count.thres.high:", count.thres.high))
#   print(paste("identifier:", identifier))                  
   print(paste("n.markers:", n.markers))   
   print(paste("locs.table.file:", locs.table.file))

# ------------------------------------------------------------------------------------------------------------------------------
   # Load libraries

   if (is.null(seed.names)) seed.names <- "NULLSEED"
   
   pdf(file=pdf.output.file, height=14, width=8.5)
   set.seed(r.seed)

   # Read table with HUGO gene symbol vs. chr location
   
   if (!is.null(locs.table.file)) {
      locs.table <- read.table(locs.table.file, header=T, sep="\t", skip=0, colClasses = "character")
    }
   
   # Define color map

   mycol <- vector(length=512, mode = "numeric")
   for (k in 1:256) mycol[k] <- rgb(255, k - 1, k - 1, maxColorValue=255)
   for (k in 257:512) mycol[k] <- rgb(511 - (k - 1), 511 - (k - 1), 255, maxColorValue=255)
   mycol <- rev(mycol)
   ncolors <- length(mycol)

   # Read datasets

   dataset.1 <- MSIG.Gct2Frame(filename = ds1)
   m.1 <- data.matrix(dataset.1$ds)
   row.names(m.1) <- dataset.1$row.names
   Ns.1 <- ncol(m.1)  
   sample.names.1 <- colnames(m.1) <- dataset.1$names

   dataset.2 <- MSIG.Gct2Frame(filename = ds2)
   m.2 <- data.matrix(dataset.2$ds)
   row.names(m.2) <- dataset.2$row.names
   Ns.2 <- ncol(m.2)  
   sample.names.2 <- colnames(m.2) <- dataset.2$names

    # exclude samples with target == NA

   target <- m.1[target.name,]
   print(paste("initial target length:", length(target)))      
   locs <- seq(1, length(target))[!is.na(target)]
   m.1 <- m.1[,locs]
   sample.names.1 <- sample.names.1[locs]
   print(paste("target length after excluding NAs:", ncol(m.1)))     

   overlap <- intersect(sample.names.1, sample.names.2)
   length(overlap)
   locs1 <- match(overlap, sample.names.1)
   locs2 <- match(overlap, sample.names.2)
   m.1 <- m.1[, locs1]
   m.2 <- m.2[, locs2]

   # Filter samples with only the selected phenotypes 

   if (!is.null(phen.selected)) {
      samples.table <- read.table(phen.table, header=T, row.names=1, sep="\t", skip=0)
      table.sample.names <- row.names(samples.table)
      locs1 <- match(colnames(m.2), table.sample.names)
      phenotype <- as.character(samples.table[locs1, phen.column])
      
      locs2 <- NULL
      for (k in 1:ncol(m.2)) {   
         if (!is.na(match(phenotype[k], phen.selected))) {
            locs2 <- c(locs2, k)
         }
      }
      length(locs2)
      m.1 <- m.1[, locs2]
      m.2 <- m.2[, locs2]
      phenotype <- phenotype[locs2]
      table(phenotype)
    }

   # Define target

   target <- m.1[target.name,]
   if (target.match == "negative") {
      ind <- order(target, decreasing=F)
   } else {
      ind <- order(target, decreasing=T)
   }
   target <- target[ind]
   m.2 <- m.2[, ind]

   if (!is.na(match(target.name, row.names(m.2)))) {
     loc <- match(target.name, row.names(m.2))
     m.2 <- m.2[-loc,]
   }

   MUT.count <- AMP.count <- DEL.count <- 0
   for (i in 1:nrow(m.2)) {
      temp <- strsplit(row.names(m.2)[i], split="_")
      temp <- strsplit(temp[[1]][length(temp[[1]])], split=" ")
      suffix <- temp[[1]][1]
      if (!is.na(suffix)) {
         if (suffix == "MUT") MUT.count <- MUT.count + 1
         if (suffix == "AMP") AMP.count <- AMP.count + 1
         if (suffix == "DEL") DEL.count <- DEL.count + 1
     }
   }
   print(paste("Initial number of features ", nrow(m.2), " MUT:", MUT.count, " AMP:", AMP.count, " DEL:", DEL.count))
   
   # Eliminate flat, sparse or features that are too dense

   if (!is.null(count.thres.low) && !is.null(count.thres.high)) {
      sum.rows <- rowSums(m.2)
      seed.flag <- rep(0, nrow(m.2))
      if (seed.names != "NULLSEED") {
         locs <- match(seed.names, row.names(m.2))
         locs <- locs[!is.na(locs)]
         seed.flag[locs] <- 1
      }
      retain <- rep(0, nrow(m.2))
      for (i in 1:nrow(m.2)) {
         if ((sum.rows[i] >= count.thres.low) && (sum.rows[i] <= count.thres.high)) retain[i] <- 1
         if (seed.flag[i] == 1) retain[i] <- 1
      }

      m.2 <- m.2[retain == 1,]
      print(paste("Number of features kept:", sum(retain), "(", signif(100*sum(retain)/length(retain), 3), " percent)"))
  }
   
   # Normalize features and define seeds

  if (normalize.features == "standardized") {
      for (i in 1:nrow(m.2)) {
         mean.row <- mean(m.2[i,])
         sd.row <- ifelse(sd(m.2[i,]) == 0, 0.1*mean.row, sd(m.2[i,]))
         m.2[i,] <- (m.2[i,] - mean.row)/sd.row
       }
   } else if (normalize.features == "0.1.rescaling") {
      for (i in 1:nrow(m.2)) {
         max.row <- max(m.2[i,])
         min.row <- min(m.2[i,])
         range.row <- ifelse(max.row == min.row, 1, max.row - min.row)
         m.2[i,] <- (m.2[i,] - min.row)/range.row
       }
    }
   
  if (seed.names == "NULLSEED") {
     seed <- as.vector(rep(0, ncol(m.2)))      
     seed.vectors <- as.matrix(t(seed))
  } else {
      print("Location(s) of seed(s):")
      print(match(seed.names, row.names(m.2)))
      if (length(seed.names) > 1) {
         seed <- apply(m.2[seed.names,], MARGIN=2, FUN=seed.combination.op)
         seed.vectors <- as.matrix(m.2[seed.names,])
      } else {
         seed <- m.2[seed.names,]
         seed.vectors <- as.matrix(t(m.2[seed.names,]))
      }
      locs <- match(seed.names, row.names(m.2))
      locs
     m.2 <- m.2[-locs,]
     dim(m.2)
   }

  if (length(table(m.2[1,])) > ncol(m.2)*0.5) { # continuous target
     feature.type <- "continuous"
  } else {
     feature.type <- "discrete"
  }
    
  # Exclude user-specified features 
   
   if (!is.null(exclude.features)) {
      locs <- match(exclude.features, row.names(m.2))
      locs <- locs[!is.na(locs)]
      m.2 <- m.2[-locs,]
    }

  #  Consolidate identical features

  # This is a very fast way to eliminate perfectly identical features compared with what we do below in "similar"
   
   if (consolidate.identical.features == "identical") {  
      dim(m.2)
      summary.vectors <- apply(m.2, MARGIN=1, FUN=paste, collapse="")
      ind <- order(summary.vectors)
      summary.vectors <- summary.vectors[ind]
      m.2 <- m.2[ind,]
      taken <- i.count <- rep(0, length(summary.vectors))
      i <- 1
      while (i <= length(summary.vectors)) {
        j <- i + 1
        while ((summary.vectors[i] == summary.vectors[j]) & (j <= length(summary.vectors))) {
            j <- j + 1
         }
        i.count[i] <- j - i
        if (i.count[i] > 1) taken[seq(i + 1, j - 1)] <- 1
        i <- j
      }
   
      if (sum(i.count) != length(summary.vectors)) stop("ERROR")     # Add counts in parenthesis
      row.names(m.2) <- paste(row.names(m.2), " (", i.count, ")", sep="")
      m.2 <- m.2[taken == 0,]
      dim(m.2)

   # This uses the hamming distance to consolidate similar features up to the Hamming dist. threshold 
      
   } else if (consolidate.identical.features == "similar") { 
      hamming.matrix <- hamming.distance(m.2)
      taken <- rep(0, nrow(m.2))
      for (i in 1:nrow(m.2)) {
         if (taken[i] == 0) { 
            similar.features <- row.names(m.2)[hamming.matrix[i,] <= cons.features.hamming.thres]
            if (length(similar.features) > 1) {
               row.names(m.2)[i]  <- paste(row.names(m.2)[i], " [", length(similar.features), "]", sep="") # Add counts in brackets
               locs <- match(similar.features, row.names(m.2))
               taken[locs] <- 1
               taken[i] <- 0
            }
        }
      }
      m.2 <- m.2[taken == 0,]
     dim(m.2)
   }

   MUT.count <- AMP.count <- DEL.count <- 0
   for (i in 1:nrow(m.2)) {
      temp <- strsplit(row.names(m.2)[i], split="_")
      temp <- strsplit(temp[[1]][length(temp[[1]])], split=" ")      
      suffix <- temp[[1]][1]
      if (!is.na(suffix)) {
         if (suffix == "MUT") MUT.count <- MUT.count + 1
         if (suffix == "AMP") AMP.count <- AMP.count + 1
         if (suffix == "DEL") DEL.count <- DEL.count + 1
     }
   }
   print(paste("Number of features (after filtering and consolidation)",
   nrow(m.2), " MUT:", MUT.count, " AMP:", AMP.count, " DEL:", DEL.count))
   
   # Add location info

   if (!is.null(locs.table.file)) {
      gene.symbol <- row.names(m.2)
      chr <- rep(" ", length(gene.symbol))
      for (i in 1:length(gene.symbol)) {
        temp1 <- strsplit(gene.symbol[i], split="_")
        temp2 <- strsplit(temp1[[1]][1], split="\\.")
        gene.symbol[i] <- ifelse(temp2[[1]][1] == "", temp1[[1]][1], temp2[[1]][1])
        loc <- match(gene.symbol[i], locs.table[,"Approved.Symbol"])
        chr[i] <- ifelse(!is.na(loc), locs.table[loc, "Chromosome"], " ")
       }
      row.names(m.2)  <- paste(row.names(m.2), " ", chr, " ", sep="")
      print(paste("Total unmatched to chromosomal locations:", sum(chr == " "), "out of ", nrow(m.2), "features"))
    }

   # Save filtered and consolidated file

    if (!is.null(save_preprocessed_features_dataset)) {
       write.gct.2(gct.data.frame = data.frame(m.2), descs = row.names(m.2), filename = save_preprocessed_features_dataset)
   }
   
   # Compute MI and % explained with original seed(s)
   
   median_target <- median(target)
    if (target.match == "negative") {
      target.locs <- seq(1, length(target))[target <= median_target]
    } else {
      target.locs <- seq(1, length(target))[target > median_target]
    }

   cmi.orig.seed <- cmi.orig.seed.cum <- pct_explained.orig.seed <- pct_explained.orig.seed.cum <- vector(length=length(seed.names), mode="numeric")
   if (length(seed.names) > 1) {
      seed.cum <- NULL
      for (i in 1:nrow(seed.vectors)) {
         y <- seed.vectors[i,]
         cmi.orig.seed[i] <- assoc(target, y, assoc.metric)
         pct_explained.orig.seed[i] <- sum(y[target.locs])/length(target.locs)
         seed.cum <- apply(rbind(seed.vectors[i,], seed.cum), MARGIN=2, FUN=seed.combination.op)
         cmi.orig.seed.cum[i] <- assoc(target, seed.cum, assoc.metric)
         pct_explained.orig.seed.cum[i] <- sum(seed.cum[target.locs])/length(target.locs)
      }
   } else {
       y <- as.vector(seed.vectors)
       seed.cum <- y
       cmi.orig.seed <- cmi.orig.seed.cum <- assoc(target, y, assoc.metric)
       pct_explained.orig.seed <- sum(y[target.locs])/length(target.locs)
   }
    cmi.seed.iter0 <- assoc(target, seed, assoc.metric)
    pct_explained.seed.iter0 <- sum(seed[target.locs])/length(target.locs) 

   # CMI iterations

   cmi <- pct_explained <- cmi.names <- matrix(0, nrow=nrow(m.2), ncol=max.n.iter)
   cmi.seed <- pct_explained.seed <- vector(length=max.n.iter, mode="numeric")
   seed.names.iter <- vector(length=max.n.iter, mode="character")
   seed.initial <- seed
   seed.iter <- matrix(0, nrow=max.n.iter, ncol=ncol(m.2))

   target.rand <- matrix(target, nrow=n.perm, ncol=ncol(m.2), byrow=TRUE)
   for (i in 1:n.perm) target.rand[i,] <- sample(target.rand[i,])

   for (iter in 1:max.n.iter) {

      cmi.rand <- matrix(0, nrow=nrow(m.2), ncol=n.perm)     
      for (k in 1:nrow(m.2)) {
         if (k %% 100 == 0) print(paste("Iter:", iter, " feature #", k, " out of ", nrow(m.2)))
         y <- m.2[k,]
         cmi[k, iter] <- cond.assoc(target, y, seed, assoc.metric)
         for (j in 1:n.perm) {
            cmi.rand[k, j] <- cond.assoc(target.rand[j,], y, seed, assoc.metric)
          }
       }

      if (target.match == "negative") {
         ind <- order(cmi[, iter], decreasing=F)
      } else {
         ind <- order(cmi[, iter], decreasing=T)
      }
      cmi[, iter] <- cmi[ind, iter]
      cmi.names[, iter] <- row.names(m.2)[ind]
      pct_explained[iter] <- sum(m.2[cmi.names[1, iter], target.locs])/length(target.locs)
      
      # Estimate p-vals and FDRs

      p.val <- FDR <- FDR.lower <- rep(0, nrow(m.2))
      for (i in 1:nrow(m.2)) p.val[i] <- sum(cmi.rand >  cmi[i, iter])/(nrow(m.2)*n.perm)
      FDR <- p.adjust(p.val, method = "fdr", n = length(p.val))
      FDR.lower <- p.adjust(1 - p.val, method = "fdr", n = length(p.val))
    
      for (i in 1:nrow(m.2)) {
         if (cmi[i, iter] < 0) {
            p.val[i] <- 1 - p.val[i]
            FDR[i] <- FDR.lower[i]
         }
         p.val[i] <- signif(p.val[i], 2)
         FDR[i] <- signif(FDR[i], 2)
      }
      p.zero.val <- paste("<", signif(1/(nrow(m.2)*n.perm), 2), sep="")
      p.val <- ifelse(p.val == 0, rep(p.zero.val, length(p.val)), p.val)

      # Make a heatmap of the n.marker top hits in this iteration

      size.mid.panel <- length(seed.names) + iter
      pad.space <- 15
      
      nf <- layout(matrix(c(1, 2, 3, 4), 4, 1, byrow=T), 1, c(2, size.mid.panel, ceiling(n.markers/2) + 4, pad.space), FALSE)
      cutoff <- 2.5
      x <- as.numeric(target)         
      x <- (x - mean(x))/sd(x)         
      ind1 <- which(x > cutoff)
      ind2 <- which(x < -cutoff)
      x[ind1] <- cutoff
      x[ind2] <- -cutoff
      V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      par(mar = c(1, 22, 2, 12))
      image(1:length(target), 1:1, as.matrix(V1), col=mycol, zlim=c(0, ncolors), axes=FALSE,
            main=paste("REVEALER - Iteration:", iter), sub = "", xlab= "", ylab="",
            font=2, family="")
      axis(2, at=1:1, labels=paste("TARGET: ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
           cex.axis=character.scaling, font.axis=1,
           line=0, font=2, family="")
      axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
      axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")
 
      if (iter == 1) {
            V0 <- rbind(seed.vectors, seed + 2)
            cmi.vals <- c(cmi.orig.seed, cmi.seed.iter0)
            cmi.vals <- signif(cmi.vals, 2)
            cmi.cols <- rep("black", length(cmi.orig.seed) + 1)                     # IC/CIC colors
            row.names(V0) <- c(paste("SEED: ", seed.names), "SUMMARY SEED:")
            V0 <- apply(V0, MARGIN=2, FUN=rev)
       } else {
         V0 <- rbind(seed.vectors, m.2[seed.names.iter[1:(iter-1)],], seed + 2)
         row.names(V0) <- c(paste("SEED:   ", seed.names), paste("ITERATION ", seq(1, iter-1), ":  ",
                                                                 seed.names.iter[1:(iter-1)], sep=""), "SUMMARY SEED:")
         cmi.vals <- c(cmi.orig.seed, cmi[1, 1:iter-1], cmi.seed[iter-1])
         cmi.vals <- signif(cmi.vals, 2)
         cmi.cols <- c(rep("black", length(cmi.orig.seed)), rep("steelblue", iter - 1), "black")      # IC/CIC colors
         pct.vals <- c(signif(pct_explained.orig.seed, 2), signif(pct_explained[seq(1, iter - 1)], 2),
                       signif(pct_explained.seed[iter - 1], 2))         
         V0 <- apply(V0, MARGIN=2, FUN=rev)
       }

      all.vals <- cmi.vals
      par(mar = c(1, 22, 0, 12))
      if (feature.type == "discrete") {
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3), col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9],
                                                                    brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5]),
               axes=FALSE, main="", sub = "", xlab= "", ylab="")
      } else { # continuous
         for (i in 1:length(V0[,1])) {
            x <- as.numeric(V0[i,])
            V0[i,] <- (x - mean(x))/sd(x)
            max.v <- max(max(V0[i,]), -min(V0[i,]))
            V0[i,] <- ceiling(ncolors * (V0[i,] - (- max.v))/(1.001*(max.v - (- max.v))))
         }
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="", sub = "",
               xlab= "", ylab="")
      }
#      axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
#      axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
      for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }
      V0 <- m.2[cmi.names[1:n.markers, iter],]
      V0 <- apply(V0, MARGIN=2, FUN=rev)
      par(mar = c(6, 22, 3, 12))
      if (feature.type == "discrete") {
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 1),
               col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9]),
               axes=FALSE, main=paste("Top", n.markers, "Matches"), sub = "", xlab= "", ylab="")
      } else { # continuous
         for (i in 1:length(V0[,1])) {
            cutoff <- 2.5
            x <- as.numeric(V0[i,])            
            x <- (x - mean(x))/sd(x)         
            ind1 <- which(x > cutoff)
            ind2 <- which(x < -cutoff)
            x[ind1] <- cutoff
            x[ind2] <- -cutoff
            V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
         }
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="", sub = "",
               xlab= "", ylab="")
      }
      axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=0.9*character.scaling,
           line=0, font=2, family="")

      all.vals <- paste(signif(cmi[1:n.markers, iter], 2), p.val[1:n.markers], FDR[1:n.markers], sep="   ")

      axis(4, at=nrow(V0)+0.4, labels=" CIC    p-val   FDR", adj= 0.5, tick=FALSE, las = 1,
           cex.axis=0.8*character.scaling, line=0, font=2, family="")
      axis(4, at=c(seq(1, nrow(V0) - 1), nrow(V0) - 0.2), labels=rev(all.vals), adj= 0.5, tick=FALSE,
           las = 1, cex.axis=0.9*character.scaling, line=0, font=2, family="")
      axis(1, at=1:ncol(V0), labels=colnames(V0), adj= 0.5, tick=FALSE,las = 3, cex=1,
           cex.axis=0.45*character.scaling,  line=0, font=2, family="")

     # second page shows the same markers clustered in groups with similar profiles

     tab <- m.2[cmi.names[1:n.markers, iter],]
     all.vals <- paste(signif(cmi[1:n.markers, iter], 2), p.val[1:n.markers], FDR[1:n.markers], sep="   ")

     # Cluster and make heatmap of n.markers top hits in groups

     tab2 <- tab + 0.001

     k.min <- 2
     k.max <- 10
     NMF.models <- nmf(tab2, seq(k.min, k.max), nrun = 25, method="brunet", seed=9876)
     plot(NMF.models)
     NMF.sum <- summary(NMF.models)

     k.vec <- seq(k.min, k.max, 1)
     cophen <- NMF.sum[, "cophenetic"]

     peak <- c(0, rep(0, k.max-2), 0)
     for (h in 2:(length(cophen) - 1)) if (cophen[h - 1] < cophen[h] & cophen[h] > cophen[h + 1]) peak[h] <- 1

     if (sum(peak) == 0) {
        if (cophen[1] > cophen[length(cophen)]) {
           k <- k.min
         } else {
           k <- k.max
         }
     } else {
        k.peaks <- k.vec[peak == 1]
        k <- rev(k.peaks)[1]
     }
     print(paste("Number of groups:", k))
     NMF.model <- nmf(tab2, k, method="brunet", seed=9876)
     classes <- predict(NMF.model, "rows")
     table(classes)
     lens <- table(classes)

     lens2 <- ifelse(lens <= 5, 5, lens)
     lens2[length(lens2)] <- lens2[length(lens2)] + 5


     def.par <- par(no.readonly = TRUE)       
     nf <- layout(matrix(seq(1, k+3), k+3, 1, byrow=T), 1, c(3.5, size.mid.panel, lens2, pad.space), FALSE)      

      cutoff <- 2.5
      x <- as.numeric(target)         
      x <- (x - mean(x))/sd(x)         
      ind1 <- which(x > cutoff)
      ind2 <- which(x < -cutoff)
      x[ind1] <- cutoff
      x[ind2] <- -cutoff
      V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      par(mar = c(1, 22, 1, 12))
      image(1:length(target), 1:1, as.matrix(V1), col=mycol, zlim=c(0, ncolors), axes=FALSE,
            main=paste("REVEALER - Iteration:", iter), sub = "", xlab= "", ylab="",
            font=2, family="")
     axis(2, at=1:1, labels=paste("TARGET: ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
          cex.axis=character.scaling, font.axis=1,
           line=0, font=2, family="")

      axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
      axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")

#      axis(4, at=1:1, labels="  IC/CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=0.8*character.scaling,
#           font.axis=1, line=0, font=2, family="")
 
      if (iter == 1) {
            V0 <- rbind(seed.vectors, seed + 2)
            cmi.vals <- c(cmi.orig.seed, cmi.seed.iter0)
            cmi.vals <- signif(cmi.vals, 2)
            row.names(V0) <- c(paste("SEED: ", seed.names), "SUMMARY SEED:")
            V0 <- apply(V0, MARGIN=2, FUN=rev)
       } else {
         V0 <- rbind(seed.vectors, m.2[seed.names.iter[1:(iter-1)],], seed + 2)
         row.names(V0) <- c(paste("SEED:   ", seed.names), paste("ITERATION ", seq(1, iter-1), ":  ",
                                        seed.names.iter[1:(iter-1)], sep=""), "SUMMARY SEED:")
         cmi.vals <- c(cmi.orig.seed, cmi[1, 1:iter-1], cmi.seed[iter-1])
         cmi.vals <- signif(cmi.vals, 2)
         pct.vals <- c(signif(pct_explained.orig.seed, 2), signif(pct_explained[seq(1, iter - 1)], 2),
                       signif(pct_explained.seed[iter - 1], 2))         
         V0 <- apply(V0, MARGIN=2, FUN=rev)
       }

      all.vals <- cmi.vals
      par(mar = c(1, 22, 0, 12))
      if (feature.type == "discrete") {
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3), col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9],
                                                                    brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5]),
               axes=FALSE, main="", sub = "", xlab= "", ylab="")
      } else { # continuous
         for (i in 1:length(V0[,1])) {
            x <- as.numeric(V0[i,])
            V0[i,] <- (x - mean(x))/sd(x)
            max.v <- max(max(V0[i,]), -min(V0[i,]))
            V0[i,] <- ceiling(ncolors * (V0[i,] - (- max.v))/(1.001*(max.v - (- max.v))))
         }
         image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="",
               sub = "", xlab= "", ylab="")
      }
      
#      axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
#      axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#           line=0, font=2, family="")
      for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }

    # Groups of abnormalities
      
     all.vals <- paste(signif(cmi[1:n.markers, iter], 2), p.val[1:n.markers], FDR[1:n.markers], sep="   ")
      
     for (h in sort(unique(classes))) {      
         if (lens[h] == 1) {
            V0 <- t(as.matrix(tab[classes == h,]))
          } else {
            V0 <- tab[classes == h,]
            V0 <- apply(V0, MARGIN=2, FUN=rev)
          }
         r.names <- row.names(tab)[classes == h]
         all.vals0 <- all.vals[classes == h]         
         if (h < k) {
            par(mar = c(0.5, 22, 1, 12))
          } else {
            par(mar = c(3, 22, 1, 12))
          }
         if (feature.type == "discrete") {
            if (lens[h] == 1) {           
               image(1:ncol(V0), 1, t(V0), zlim = c(0, 1), col=c(brewer.pal(9, "Blues")[3],
                                                               brewer.pal(9, "Blues")[9]),
                     axes=FALSE, main=paste("Top Matches. Group:", h, "(iter ", iter, ")"), sub = "",
                     xlab= "", ylab="", cex.main=0.8)
             } else {
               image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 1), col=c(brewer.pal(9, "Blues")[3],
                                                                        brewer.pal(9, "Blues")[9]),
                     axes=FALSE, main=paste("Top Matches. Group:", h, "(iter ", iter, ")"),
                     sub = "", xlab= "", ylab="", cex.main=0.8)
             }
         } else { # continuous
            for (i in 1:length(V0[,1])) {
               cutoff <- 2.5
               x <- as.numeric(V0[i,])            
               x <- (x - mean(x))/sd(x)         
               ind1 <- which(x > cutoff)
               ind2 <- which(x < -cutoff)
               x[ind1] <- cutoff
               x[ind2] <- -cutoff
               V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
            }
            image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE,
               main=paste("Top Matches. Group:", h), sub = "", xlab= "", ylab="")
         }

         if (lens[h] == 1) {
           axis(2, at=1, labels=rev(r.names), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
                line=-0.7, font=2, family="")
           axis(4, at=1+0.4, labels=" CIC     p-val     FDR", adj= 0.5, tick=FALSE, las = 1,
                cex.axis=0.8*character.scaling, line=-0.7, font=2, family="")
           axis(4, at=1, labels=all.vals0, adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
                line=-0.7, font=2, family="")
         } else {
            axis(2, at=1:nrow(V0), labels=rev(r.names), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
                 line=-0.7, font=2, family="")
            axis(4, at=nrow(V0)+0.4, labels=" CIC     p-val     FDR", adj= 0.5, tick=FALSE, las = 1,
                 cex.axis=0.8*character.scaling, line=-0.7, font=2, family="")            
            axis(4, at=c(seq(1, nrow(V0) - 1), nrow(V0) - 0.2), labels=rev(all.vals0), adj= 0.5,
                 tick=FALSE, las = 1, cex.axis=character.scaling, line=-0.7, font=2, family="")
         }
      }
      par(def.par)
      
      # Update seed

      seed.names.iter[iter] <- cmi.names[1, iter] # top hit from this iteration
      seed <- apply(rbind(seed, m.2[seed.names.iter[iter],]), MARGIN=2, FUN=seed.combination.op)
      seed.iter[iter,] <- seed
      cmi.seed[iter] <- assoc(target, seed, assoc.metric)
      pct_explained.seed[iter] <- sum(seed[target.locs])/length(target.locs)
      
    } # end of iterations loop

   # Final summary figures -----------------------------------------------------------------------------

   summ.panel <- length(seed.names) + 2 * max.n.iter + 2
  
   legend.size <- 4
   pad.space <- 30 - summ.panel - legend.size

   nf <- layout(matrix(c(1, 2, 3, 0), 4, 1, byrow=T), 1, c(2, summ.panel, legend.size, pad.space), FALSE)

   cutoff <- 2.5
   x <- as.numeric(target)         
   x <- (x - mean(x))/sd(x)         
   ind1 <- which(x > cutoff)
   ind2 <- which(x < -cutoff)
   x[ind1] <- cutoff
   x[ind2] <- -cutoff
   V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))

   par(mar = c(1, 22, 2, 12))
   image(1:length(target), 1:1, as.matrix(V1), zlim=c(0, ncolors), col=mycol, axes=FALSE,
         main=paste("REVEALER - Results"), sub = "", xlab= "", ylab="", font=2, family="")
  
   axis(2, at=1:1, labels=paste("TARGET:  ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
        cex.axis=character.scaling,  line=0, font=2, family="")

   axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
   axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")

#   axis(4, at=1:1, labels="  IC   ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")

   V0 <- rbind(seed.vectors + 2, seed.cum) 
   for (i in 1:max.n.iter) {
      V0 <- rbind(V0,
                  m.2[seed.names.iter[i],] + 2,
                  seed.iter[i,])
   }

   row.names.V0 <- c(paste("SEED:   ", seed.names), "SUMMARY SEED:")
   for (i in 1:max.n.iter) {
      row.names.V0 <- c(row.names.V0, paste("ITERATION ", i, ":  ", seed.names.iter[i], sep=""),
                        paste("SUMMARY FEATURE ", i, ":  ", sep=""))
   }
   row.names(V0) <- row.names.V0

   cmi.vals <- c(cmi.orig.seed, cmi.orig.seed.cum[length(seed.names)])                 
   for (i in 1:max.n.iter) {
      cmi.vals <- c(cmi.vals, as.vector(cmi[1, i]), cmi.seed[i])
    }
   cmi.vals <- signif(cmi.vals, 2)
   all.vals <-cmi.vals

   cmi.cols <- c(rep("black", length(cmi.orig.seed)), "black", rep(c("steelblue", "black"), max.n.iter))                     # IC/CIC colors   

   V0 <- apply(V0, MARGIN=2, FUN=rev)

   par(mar = c(7, 22, 0, 12))
   if (feature.type == "discrete") {  
       image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3),
       col=c(brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5],                          
             brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9]), axes=FALSE, main="",
             sub = "", xlab= "", ylab="")
   } else { # continuous
      for (i in 1:nrow(V0)) {
         cutoff <- 2.5
         x <- as.numeric(V0[i,])
         x <- (x - mean(x))/sd(x)         
         ind1 <- which(x > cutoff)
         ind2 <- which(x < -cutoff)
         x[ind1] <- cutoff
         x[ind2] <- -cutoff
         V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      }
      image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="",
            sub = "", xlab= "", ylab="")
   }
#   axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
#   axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
    for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }
   
   axis(1, at=1:ncol(V0), labels=colnames(V0), adj= 0.5, tick=FALSE,las = 3, cex=1, cex.axis=0.4*character.scaling,
        line=0, font=2, family="")

        # Legend

      par.mar <- par("mar")
      par(mar = c(3, 35, 8, 10))
      leg.set <- seq(-cutoff, cutoff, 2*cutoff/100)
      image(1:101, 1:1, as.matrix(leg.set), zlim=c(-cutoff, cutoff), col=mycol, axes=FALSE, main="",
          sub = "", xlab= "", ylab="",font=2, family="")
      ticks <- c(-2, -1, 0, 1, 2)
      tick.cols <- rep("black", 5)
      tick.lwd <- c(1,1,2,1,1)
      locs <- NULL
      for (k in 1:length(ticks)) locs <- c(locs, which.min(abs(ticks[k] - leg.set)))
      axis(1, at=locs, labels=ticks, adj= 0.5, tick=T, cex=0.8, cex.axis=1, line=0, font=2, family="")
      mtext("Standardized Target Profile", cex=0.8, side = 1, line = 3.5, outer=F)
      par(mar = par.mar)

   V0 <- rbind(target, seed.vectors, seed.cum) 
   for (i in 1:max.n.iter) {
      V0 <- rbind(V0,
                  m.2[seed.names.iter[i],],
                  seed.iter[i,])
   }
   V0.colnames <- colnames(V0)
   V0 <- cbind(V0, c(1, all.vals))
   colnames(V0) <- c(V0.colnames, "IC")

   row.names.V0 <- c(target.name, seed.names, "SUMMARY SEED:")
   for (i in 1:max.n.iter) {
      row.names.V0 <- c(row.names.V0, seed.names.iter[i], paste("SUMMARY FEATURE ", i, ":  ", sep=""))
   }
   row.names(V0) <- row.names.V0
  
  # Version without summaries ----------------------------------------------------

   summ.panel <- length(seed.names) + max.n.iter + 2
  
   legend.size <- 4
   pad.space <- 30 - summ.panel - legend.size
   
   nf <- layout(matrix(c(1, 2, 3, 0), 4, 1, byrow=T), 1, c(2, summ.panel, legend.size, pad.space), FALSE)

   cutoff <- 2.5
   x <- as.numeric(target)         
   x <- (x - mean(x))/sd(x)         
   ind1 <- which(x > cutoff)
   ind2 <- which(x < -cutoff)
   x[ind1] <- cutoff
   x[ind2] <- -cutoff
   V1 <- ceiling(ncolors * (x + cutoff)/(cutoff*2))

   par(mar = c(1, 22, 2, 12))
   image(1:length(target), 1:1, as.matrix(V1), zlim=c(0, ncolors), col=mycol, axes=FALSE,
         main=paste("REVEALER - Results"),
         sub = "", xlab= "", ylab="", font=2, family="")
  
   axis(2, at=1:1, labels=paste("TARGET:  ", target.name), adj= 0.5, tick=FALSE,las = 1, cex=1,
        cex.axis=character.scaling,  line=0, font=2, family="")
   
#   axis(4, at=1:1, labels="  IC   ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")

      axis(4, at=1:1, labels="  IC ", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="black")
      axis(4, at=1:1, labels="       / CIC", adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,     # IC/CIC
           font.axis=1, line=0, font=2, family="", col.axis="steelblue")

   
   V0 <- seed.vectors + 2
   for (i in 1:max.n.iter) {
      V0 <- rbind(V0,
                  m.2[seed.names.iter[i],] + 2)
   }
   V0 <- rbind(V0, seed.iter[max.n.iter,])

   row.names.V0 <- c(paste("SEED:   ", seed.names))
   for (i in 1:max.n.iter) {
      row.names.V0 <- c(row.names.V0, paste("ITERATION ", i, ":  ", seed.names.iter[i], sep=""))
   }
   row.names(V0) <- c(row.names.V0, "FINAL SUMMARY")

   cmi.vals <- cmi.orig.seed
   for (i in 1:max.n.iter) {
      cmi.vals <- c(cmi.vals, as.vector(cmi[1, i]))
    }
   cmi.vals <- c(cmi.vals, cmi.seed[max.n.iter])
   cmi.vals <- signif(cmi.vals, 2)
   all.vals <-cmi.vals

   cmi.cols <- c(rep("black", length(cmi.orig.seed)), rep("steelblue", max.n.iter), "black")    # IC/CIC colors   
      
   V0 <- apply(V0, MARGIN=2, FUN=rev)
   par(mar = c(7, 22, 0, 12))   
   
   if (feature.type == "discrete") {  
       image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, 3),
       col=c(brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5],                          
              brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9]), axes=FALSE, main="",
             sub = "", xlab= "", ylab="")
   } else { # continuous
      for (i in 1:nrow(V0)) {
         cutoff <- 2.5
         x <- as.numeric(V0[i,])
         x <- (x - mean(x))/sd(x)         
         ind1 <- which(x > cutoff)
         ind2 <- which(x < -cutoff)
         x[ind1] <- cutoff
         x[ind2] <- -cutoff
         V0[i,] <- ceiling(ncolors * (x + cutoff)/(cutoff*2))
      }
      image(1:ncol(V0), 1:nrow(V0), t(V0), zlim = c(0, ncolors), col=mycol, axes=FALSE, main="",
            sub = "", xlab= "", ylab="")
   }
#   axis(2, at=1:nrow(V0), labels=row.names(V0), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
#   axis(4, at=1:nrow(V0), labels=rev(all.vals), adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
#        line=0, font=2, family="")
      for (axis.i in 1:nrow(V0)) {
          axis(2, at=axis.i, labels=row.names(V0)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
          axis(4, at=axis.i, labels=rev(all.vals)[axis.i], adj= 0.5, tick=FALSE, las = 1, cex.axis=character.scaling,
               line=0, font=2, family="", col.axis = rev(cmi.cols)[axis.i])
      }
   
   axis(1, at=1:ncol(V0), labels=colnames(V0), adj= 0.5, tick=FALSE,las = 3, cex=1, cex.axis=0.4*character.scaling,
        line=0, font=2, family="")

        # Legend

      par.mar <- par("mar")
      par(mar = c(3, 35, 8, 10))
      leg.set <- seq(-cutoff, cutoff, 2*cutoff/100)
      image(1:101, 1:1, as.matrix(leg.set), zlim=c(-cutoff, cutoff), col=mycol, axes=FALSE, main="",
          sub = "", xlab= "", ylab="",font=2, family="")
      ticks <- c(-2, -1, 0, 1, 2)
      tick.cols <- rep("black", 5)
      tick.lwd <- c(1,1,2,1,1)
      locs <- NULL
      for (k in 1:length(ticks)) locs <- c(locs, which.min(abs(ticks[k] - leg.set)))
      axis(1, at=locs, labels=ticks, adj= 0.5, tick=T, cex=0.8, cex.axis=0.8, line=0, font=2, family="")
      mtext("Standardized Target Profile", cex=0.8, side = 1, line = 3.5, outer=F)
      par(mar = par.mar)

  # Landscape plot ---------------------------------------------------------------

#  if (produce.lanscape.plot == T) {
#  
#   nf <- layout(matrix(c(1, 2), 2, 1, byrow=T), 1, c(2, 1), FALSE)
#
#    if (length(as.vector(cmi.names[1:top.n, 1:max.n])) > 1) {
#       V0 <- rbind(seed.vectors, as.matrix(m.2[as.vector(cmi.names[1:top.n, 1:max.n]),]))
#    } else {
#       V0 <- rbind(seed.vectors, t(as.matrix(m.2[as.vector(cmi.names[1:top.n, 1:max.n]),])))
#    }
#   
#   number.seq <- NULL
#   for (i in 1:max.n) number.seq <- c(number.seq, rep(i, top.n))
#
#   row.names(V0) <-  c(paste("SEED: ", seed.names, "(", signif(cmi.orig.seed, 2), ")"),
#                       paste("ITER ", number.seq, ":", as.vector(cmi.names[1:top.n, 1:max.n]),
#                             "(", signif(as.vector(cmi[1:top.n, 1:max.n]), 2), ")"))
# 
#    cmi.vals <- c(cmi.orig.seed, as.vector(cmi[1:top.n, 1:max.n]))
#
#   total.points <- row(V0)
#   V2 <- V0
#   metric.matrix <- matrix(0, nrow=nrow(V2), ncol=nrow(V2))
#   row.names(metric.matrix)  <- row.names(V2)
#   colnames(metric.matrix) <- row.names(V2)
#   MI.ref <- cmi.vals
#   for (i in 1:nrow(V2)) {
#      for (j in 1:i) {
#           metric.matrix[i, j] <- assoc (V2[j,], V2[i,], assoc.metric)
#      }
#   }
#   metric.matrix
#   metric.matrix <- metric.matrix + t(metric.matrix)
#   metric.matrix
#   alpha <- 5
#   metric.matrix2 <- 1 - ((1/(1+exp(-alpha*metric.matrix))))
#   for (i in 1:nrow(metric.matrix2)) metric.matrix2[i, i] <- 0
#   metric.matrix2
# 
#   smacof.map <- smacofSphere(metric.matrix2, ndim = 2, weightmat = NULL, init = NULL,
#                                     ties = "primary", verbose = FALSE, modulus = 1, itmax = 1000, eps = 1e-6)
#   x0 <- smacof.map$conf[,1]
#   y0 <- smacof.map$conf[,2]
#   r <- sqrt(x0*x0 + y0*y0)
#   radius <-  1 - ((1/(1+exp(-alpha*MI.ref))))
#   x <- x0*radius/r
#   y <- y0*radius/r
#   angles <- atan2(y0, x0)
#   
#   par(mar = c(4, 7, 4, 7))
# 
#   plot(x, y, pch=20, bty="n", xaxt='n', axes = FALSE, type="n", xlab="", ylab="",
#        main=paste("REVEALER - Landscape for ", target.name),
#        xlim=1.2*c(-max(radius), max(radius)), ylim=1.2*c(-max(radius), max(radius)))
#   line.angle <- seq(0, 2*pi-0.001, 0.001)
#   for (i in 1:length(x)) {
#      line.max.x <- radius[i] * cos(line.angle)
#      line.max.y <- radius[i] * sin(line.angle)
#      points(line.max.x, line.max.y, type="l", col="gray80", lwd=1)
#      points(c(0, x[i]), c(0, y[i]), type="l", col="gray80", lwd=1)
#   }
#   line.max.x <- 1.2*max(radius) * cos(line.angle)
#   line.max.y <- 1.2*max(radius) * sin(line.angle)
#   points(line.max.x, line.max.y, type="l", col="purple", lwd=2)
#   points(0, 0, pch=21, bg="red", col="black", cex=2.5)   
#   points(x, y, pch=21, bg="steelblue", col="black", cex=2.5)
#
#   x <- c(0, x)
#   y <- c(0, y)
# 
#   text(x[1], y[1], labels=print.names[1], pos=2, cex=0.85, col="red", offset=1, font=2, family="")   
#   for (i in 2:length(x)) {
#      pos <- ifelse(x[i] <= 0.25, 4, 2)
#     text(x[i], y[i], labels=print.names[i], pos=pos, cex=0.50, col="darkblue", offset=1, font=2, family="")   
#    }
#
#  }
   
   dev.off()
}

assoc <- function(x, y, metric) {

# Pairwise association of x and y
                                        
    if (length(unique(x)) == 1 || length(unique(y)) == 1) return(0)
    if (metric == "IC") {
       return(mutual.inf.v2(x = x, y = y, n.grid=25)$IC)
    } else if (metric == "COR") {
        return(cor(x, y))
    }
}

cond.assoc <-  function(x, y, z, metric) { # Association of a and y given z
#
# Conditional mutual information I(x, y | z)
#
    if (length(unique(x)) == 1 || length(unique(y)) == 1) return(0)

    if (length(unique(z)) == 1) {  # e.g. for NULLSEED
       if (metric == "IC") {
          return(mutual.inf.v2(x = x, y = y, n.grid = 25)$IC)
       } else if (metric == "COR") {
          return(cor(x, y))
       }
   } else {
       if (metric == "IC") {
          return(cond.mutual.inf(x = x, y = y, z = z, n.grid = 25)$CIC)
       } else if (metric == "COR") {
          return(pcor.test(x, y, z)$estimate)
       }
   }
}

mutual.inf.v2 <- function(x, y, n.grid=25, delta = c(bcv(x), bcv(y))) {
#
# Computes the Mutual Information/Information Coefficient IC(x, y)
#
   # Compute correlation-dependent bandwidth

   rho <- cor(x, y)
   rho2 <- abs(rho)
   delta <- delta*(1 + (-0.75)*rho2)

   # Kernel-based prob. density
   
   kde2d.xy <- kde2d(x, y, n = n.grid, h = delta)
   FXY <- kde2d.xy$z + .Machine$double.eps
   dx <- kde2d.xy$x[2] - kde2d.xy$x[1]
   dy <- kde2d.xy$y[2] - kde2d.xy$y[1]
   PXY <- FXY/(sum(FXY)*dx*dy)
   PX <- rowSums(PXY)*dy
   PY <- colSums(PXY)*dx
   HXY <- -sum(PXY * log(PXY))*dx*dy
   HX <- -sum(PX * log(PX))*dx
   HY <- -sum(PY * log(PY))*dy

   PX <- matrix(PX, nrow=n.grid, ncol=n.grid)
   PY <- matrix(PY, byrow = TRUE, nrow=n.grid, ncol=n.grid)

   MI <- sum(PXY * log(PXY/(PX*PY)))*dx*dy
   rho <- cor(x, y)
   SMI <- sign(rho) * MI

   # Use peason correlation the get the sign (directionality)   
   
   IC <- sign(rho) * sqrt(1 - exp(- 2 * MI)) 
   
   NMI <- sign(rho) * ((HX + HY)/HXY - 1)  

   return(list(MI=MI, SMI=SMI, HXY=HXY, HX=HX, HY=HY, NMI=NMI, IC=IC))
}


cond.mutual.inf <- function(x, y, z, n.grid=25, delta = 0.25*c(bcv(x), bcv(y), bcv(z))) {
 # Computes the Conditional mutual imnformation: 
 # I(X, Y | X) = H(X, Z) + H(Y, Z) - H(X, Y, Z) - H(Z)
 # The 0.25 in front of the bandwidth is because different conventions between bcv and kde3d

   # Compute correlation-dependent bandwidth
    
   rho <- cor(x, y)
   rho2 <- ifelse(rho < 0, 0, rho)
   delta <- delta*(1 + (-0.75)*rho2)

   # Kernel-based prob. density
   
   kde3d.xyz <- kde3d(x=x, y=y, z=z, h=delta, n = n.grid)
   X <- kde3d.xyz$x
   Y <- kde3d.xyz$y
   Z <- kde3d.xyz$z
   PXYZ <- kde3d.xyz$d + .Machine$double.eps
   dx <- X[2] - X[1]
   dy <- Y[2] - Y[1]
   dz <- Z[2] - Z[1]

   # Normalize density and calculate marginal densities and entropies
   
   PXYZ <- PXYZ/(sum(PXYZ)*dx*dy*dz)
   PXZ <- colSums(aperm(PXYZ, c(2,1,3)))*dy
   PYZ <- colSums(PXYZ)*dx
   PZ <- rowSums(aperm(PXYZ, c(3,1,2)))*dx*dy
   PXY <- colSums(aperm(PXYZ, c(3,1,2)))*dz
   PX <- rowSums(PXYZ)*dy*dz
   PY <- rowSums(aperm(PXYZ, c(2,1,3)))*dx*dz
   
   HXYZ <- - sum(PXYZ * log(PXYZ))*dx*dy*dz
   HXZ <- - sum(PXZ * log(PXZ))*dx*dz
   HYZ <- - sum(PYZ * log(PYZ))*dy*dz
   HZ <-  - sum(PZ * log(PZ))*dz
   HXY <- - sum(PXY * log(PXY))*dx*dy   
   HX <-  - sum(PX * log(PX))*dx
   HY <-  - sum(PY * log(PY))*dy

   MI <- HX + HY - HXY   
   CMI <- HXZ + HYZ - HXYZ - HZ

   SMI <- sign(rho) * MI
   SCMI <- sign(rho) * CMI

   IC <- sign(rho) * sqrt(1 - exp(- 2 * MI))
   CIC <- sign(rho) * sqrt(1 - exp(- 2 * CMI))
   
   return(list(CMI=CMI, MI=MI, SCMI=SCMI, SMI=SMI, HXY=HXY, HXYZ=HXYZ, IC=IC, CIC=CIC))
 }

MSIG.Gct2Frame <- function(filename = "NULL") { 
#
# Read a gene expression dataset in GCT format and converts it into an R data frame
#
   ds <- read.delim(filename, header=T, sep="\t", skip=2, row.names=1, blank.lines.skip=T, comment.char="", as.is=T, na.strings = "")
   descs <- ds[,1]
   ds <- ds[-1]
   row.names <- row.names(ds)
   names <- names(ds)
   return(list(ds = ds, row.names = row.names, descs = descs, names = names))
}

write.gct.2 <- function(gct.data.frame, descs = "", filename)
#
# Write output GCT file
#
{
    f <- file(filename, "w")
    cat("#1.2", "\n", file = f, append = TRUE, sep = "")
    cat(dim(gct.data.frame)[1], "\t", dim(gct.data.frame)[2], "\n", file = f, append = TRUE, sep = "")
    cat("Name", "\t", file = f, append = TRUE, sep = "")
    cat("Description", file = f, append = TRUE, sep = "")

    colnames <- colnames(gct.data.frame)
    cat("\t", colnames[1], file = f, append = TRUE, sep = "")

    if (length(colnames) > 1) {
       for (j in 2:length(colnames)) {
           cat("\t", colnames[j], file = f, append = TRUE, sep = "")
       }
     }
    cat("\n", file = f, append = TRUE, sep = "\t")

    oldWarn <- options(warn = -1)
    m <- matrix(nrow = dim(gct.data.frame)[1], ncol = dim(gct.data.frame)[2] +  2)
    m[, 1] <- row.names(gct.data.frame)
    if (length(descs) > 1) {
        m[, 2] <- descs
    } else {
        m[, 2] <- row.names(gct.data.frame)
    }
    index <- 3
    for (i in 1:dim(gct.data.frame)[2]) {
        m[, index] <- gct.data.frame[, i]
        index <- index + 1
    }
    write.table(m, file = f, append = TRUE, quote = FALSE, sep = "\t", eol = "\n", col.names = FALSE, row.names = FALSE)
    close(f)
    options(warn = 0)

}

  SE_assoc <- function(x, y) {
           return(2 - sqrt(mean((x - y)^2)))
       }

REVEALER_assess_features.v1 <- function(
      input_dataset,
      target,
      direction              = "positive",
      feature.files,                           
      features,                                
      output.file,
      description            = "",
      sort.by.target         = T,
      character.scaling      = 1.5,
      n.perm                 = 10000,
      create.feature.summary = F,
      feature.combination.op = "max")
 {
   suppressPackageStartupMessages(library(maptools))
   suppressPackageStartupMessages(library(RColorBrewer))

   set.seed(5209761)

   missing.value.color <- "khaki1"
   mycol <- vector(length=512, mode = "numeric")
   for (k in 1:256) mycol[k] <- rgb(255, k - 1, k - 1, maxColorValue=255)
   for (k in 257:512) mycol[k] <- rgb(511 - (k - 1), 511 - (k - 1), 255, maxColorValue=255)
   mycol <- rev(mycol)
   max.cont.color <- 512
   mycol <- c(mycol,
              missing.value.color)                  # Missing feature color

   categ.col <- c("#9DDDD6", # dusty green
                     "#F0A5AB", # dusty red
                     "#9AC7EF", # sky blue
                     "#F970F9", # violet
                     "#FFE1DC", # clay
                     "#FAF2BE", # dusty yellow
                     "#AED4ED", # steel blue
                     "#C6FA60", # green
                     "#D6A3FC", # purple
                     "#FC8962", # red
                     "#F6E370", # orange
                     "#F0F442", # yellow
                     "#F3C7F2", # pink
                     "#D9D9D9", # grey
                     "#FD9B85", # coral
                     "#7FFF00", # chartreuse
                     "#FFB90F", # goldenrod1
                     "#6E8B3D", # darkolivegreen4
                     "#8B8878", # cornsilk4
                     "#7FFFD4") # aquamarine

   cex.size.table <- c(1, 1, 1, 1, 1, 1, 1, 1, 1, 0.9,   # 1-10 characters
                       0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, # 11-20 characters
                       0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8)

   pdf(file=output.file, height=14, width=11)


   n.panels <- length(feature.files) 
   l.panels <- NULL
   for (l in 1:n.panels) l.panels <- c(l.panels, 1.5, length(features[[l]]))
   l.panels[l.panels < 2] <- 1.5
   empty.panel <- 30 - sum(unlist(l.panels))
   l.panels <- c(l.panels, empty.panel)
   n.panels <- length(l.panels)

   nf <- layout(matrix(c(seq(1, n.panels - 1), 0), n.panels, 1, byrow=T), 1, l.panels,  FALSE)
                      
   for (f in 1:length(feature.files)) {   # loop over feature types
      dataset <- MSIG.Gct2Frame(filename = input_dataset)
      m.1 <- data.matrix(dataset$ds)
      sample.names.1 <- colnames(m.1)
      Ns.1 <- ncol(m.1)

      target.vec <- m.1[target,]

      non.nas <- !is.na(target.vec)

      target.vec <- target.vec[non.nas]
      sample.names.1 <- sample.names.1[non.nas]
      Ns.1 <- length(target.vec)
      m.1 <- m.1[, non.nas]

      dataset.2 <- MSIG.Gct2Frame(filename = feature.files[[f]])
      m.2 <- data.matrix(dataset.2$ds)
      dim(m.2)
      row.names(m.2) <- dataset.2$row.names
      Ns.2 <- ncol(m.2)  
      sample.names.2 <- colnames(m.2) <- dataset.2$names
      
      overlap <- intersect(sample.names.1, sample.names.2)
      locs1 <- match(overlap, sample.names.1)
      locs2 <- match(overlap, sample.names.2)
      m.1 <- m.1[, locs1]
      target.vec <- target.vec[locs1]
      m.2 <- m.2[, locs2]
      Ns.1 <- ncol(m.1)
      Ns.2 <- ncol(m.2)
      sample.names.1 <- colnames(m.1)
      sample.names.2 <- colnames(m.2)
      
      if (sort.by.target == T) {
         if (direction == "positive") {
            ind <- order(target.vec, decreasing=T)
         } else {
            ind <- order(target.vec, decreasing=F)
         }
         target.vec <- target.vec[ind]
         sample.names.1 <- sample.names.1[ind]
         m.1 <- m.1[, ind]
         m.2 <- m.2[, ind]         
         sample.names.2 <- sample.names.2[ind]
      }

    # normalize target
      target.vec.orig <- target.vec
      unique.target.vals <- unique(target.vec)
      n.vals <- length(unique.target.vals)
      if (n.vals >= length(target.vec)*0.5) {    # Continuous value color map        
         cutoff <- 2.5
         x <- target.vec
         x <- (x - mean(x))/sd(x)         
         x[x > cutoff] <- cutoff
         x[x < - cutoff] <- - cutoff      
         x <- ceiling((max.cont.color - 1) * (x + cutoff)/(cutoff*2)) + 1
         target.vec <- x
      }
      if (f == 1) {
          main <- description
      } else {
          main <- ""
      }
      par(mar = c(0, 22, 2, 9))

      target.nchar <- ifelse(nchar(target) > 30, 30, nchar(target))
      cex.axis <- cex.size.table[target.nchar]
      
      if (n.vals >= length(target.vec)*0.5) {    # Continuous value color map        
          image(1:Ns.1, 1:1, as.matrix(target.vec), zlim = c(0, max.cont.color), col=mycol[1: max.cont.color],
                axes=FALSE, main=main, sub = "", xlab= "", ylab="")
      } else if (n.vals == 2) {  # binary
         image(1:Ns.1, 1:1, as.matrix(target.vec), zlim = range(target.vec), col=brewer.pal(9, "Blues")[3],
               brewer.pal(9, "Blues")[9],
               axes=FALSE, main=main, sub = "", xlab= "", ylab="")
      } else {  # categorical
         image(1:Ns.1, 1:1, as.matrix(target.vec), zlim = range(target.vec), col=categ.col[1:n.vals],
               axes=FALSE, main=main, sub = "", xlab= "", ylab="")
      }
      axis(2, at=1:1, labels=target, adj= 0.5, tick=FALSE, las = 1, cex=1, cex.axis=cex.axis*character.scaling,
           font.axis=1, line=0, font=2, family="")
      axis(4, at=1:1, labels=paste("   IC       p-val"), adj= 0.5, tick=FALSE, las = 1, cex=1,
           cex.axis=0.7*character.scaling,
           font.axis=1, line=0, font=2, family="")

      feature.mat <- feature.names <- NULL
      
     for (feat.n in 1:length(features[[f]])) { 
        len <- length(unlist(features[[f]][feat.n]))         
        feature.name <- unlist(features[[f]][feat.n])

        if (is.na(match(feature.name, row.names(m.2)))) next
        feature.mat <- rbind(feature.mat,  m.2[feature.name,])
        feature.names <- c(feature.names, feature.name)
      }
      feature.mat <- as.matrix(feature.mat)
      row.names(feature.mat) <- feature.names

      if (create.feature.summary == T) {
         summary.feature <- apply(feature.mat, MARGIN=2, FUN=feature.combination.op) + 2
         feature.mat <- rbind(feature.mat, summary.feature)
         row.names(feature.mat) <- c(feature.names, "SUMMARY FEATURE")
     }

      for (i in 1:nrow(feature.mat)) {

           feature.vec <- feature.mat[i,]
           unique.feature.vals <- unique(sort(feature.vec))
           non.NA.vals <- sum(!is.na(feature.vec))
           n.vals <- length(unique.feature.vals)
           if (n.vals > 2) {    # Continuous value color map        
              feature.vals.type <- "continuous"
              cutoff <- 2.5
              x <- feature.vec
              locs.non.na <- !is.na(x)
              x.nonzero <- x[locs.non.na]
              x.nonzero <- (x.nonzero - mean(x.nonzero))/sd(x.nonzero)         
              x.nonzero[x.nonzero > cutoff] <- cutoff
              x.nonzero[x.nonzero < - cutoff] <- - cutoff      
              feature.vec[locs.non.na] <- x.nonzero
              feature.vec2 <- feature.vec
              feature.vec[locs.non.na] <- ceiling((max.cont.color - 2) * (feature.vec[locs.non.na] + cutoff)/(cutoff*2)) + 1
              feature.vec[is.na(x)] <- max.cont.color + 1
              feature.mat[i,] <- feature.vec              
           }
      }
      feature.mat <- as.matrix(feature.mat)

      # compute IC association with target

      IC.vec <- p.val.vec <- stats.vec <- NULL
      sqr_error.vec <- roc.vec <- NULL
      
      for (i in 1:nrow(feature.mat)) {
           feature.vec <- feature.mat[i,]
#           IC <- IC.v1(target.vec, feature.vec)
           IC <- mutual.inf.v2(x = target.vec.orig, y = feature.vec, n.grid=25)$IC

           feature.vec.0.1 <- (feature.vec - min(feature.vec))/(max(feature.vec) - min(feature.vec))           
           target.vec.0.1 <- (target.vec - min(target.vec))/(max(target.vec) - min(target.vec))
           if (direction == "negative") target.vec.0.1 <- 1 - target.vec.0.1
           sqr_error <- SE_assoc(target.vec.0.1, feature.vec.0.1)           
           roc <- roc.area(feature.vec.0.1, target.vec.0.1)$A
           IC <- signif(IC, 3)
           null.IC <- vector(length=n.perm, mode="numeric")

       for (h in 1:n.perm) null.IC[h] <- mutual.inf.v2(x = sample(target.vec.orig), y = feature.vec, n.grid=25)$IC
           if (IC >= 0) {
             p.val <- sum(null.IC >= IC)/n.perm
           } else {
             p.val <- sum(null.IC <= IC)/n.perm
           }
           p.val <- signif(p.val, 3)
           if (p.val == 0) {
             p.val <- paste("<", signif(1/n.perm, 3), sep="")
           }

           IC.vec <- c(IC.vec, IC)
           sqr_error.vec <- c(sqr_error.vec, sqr_error)
           roc.vec <- c(roc.vec, roc)
           p.val.vec <- c(p.val.vec, p.val)
           space.chars <- "           "
           IC.char <- nchar(IC)
           pad.char <- substr(space.chars, 1, 10 - IC.char)
           stats.vec <- c(stats.vec, paste(IC, pad.char, p.val, sep=""))
       }

      if (nrow(feature.mat) > 1) {
          V <- apply(feature.mat, MARGIN=2, FUN=rev)
      } else {
          V <- as.matrix(feature.mat)
      }
      
      features.max.nchar <- max(nchar(row.names(V)))
      features.nchar <- ifelse(features.max.nchar > 30, 30, features.max.nchar)
      cex.axis <- cex.size.table[features.nchar]

      par(mar = c(1, 22, 1, 9))
      
     if (n.vals > 2) {    # Continuous value color map        
         image(1:dim(V)[2], 1:dim(V)[1], t(V), zlim = c(0, max.cont.color + 3), col=mycol, axes=FALSE, main=main,
               cex.main=0.8, sub = "", xlab= "", ylab="")
     } else {  # binary
         image(1:dim(V)[2], 1:dim(V)[1], t(V), zlim = c(0, 3), col=c(brewer.pal(9, "Blues")[3], brewer.pal(9, "Blues")[9],
                                                                    brewer.pal(9, "Greys")[2], brewer.pal(9, "Greys")[5]),
               axes=FALSE, main="", cex.main=0.8,  sub = "", xlab= "", ylab="")
     }
     axis(2, at=1:dim(V)[1], labels=row.names(V), adj= 0.5, tick=FALSE, las = 1, cex=1, cex.axis=cex.axis*character.scaling,
           font.axis=1, line=0, font=2, family="")
     axis(4, at=1:dim(V)[1], labels=rev(stats.vec), adj= 0.5, tick=FALSE, las = 1, cex=1, cex.axis=0.7*character.scaling,
           font.axis=1, line=0, font=2, family="")
     }
   dev.off()
   
}


args <- commandArgs(trailingOnly=TRUE)

option_list <- list(
  make_option("--ds1", dest="ds1"),
  make_option("--target.name", dest="target.name"),
  make_option("--target.match", dest="target.match"),
  make_option("--ds2", dest="ds2"),
  make_option("--seed.names", dest="seed.names"),            
  make_option("--max.n.iter", dest="max.n.iter"),
#  make_option("--identifier", dest="identifier"),
#  make_option("--n.perm", dest="n.perm"),
  make_option("--n.markers", dest="n.markers"),
  make_option("--locs.table.file", dest="locs.table.file"),
#  make_option("--exclude.features", dest="exclude.features"),
  make_option("--count.thres.low", dest="count.thres.low"),                                
  make_option("--count.thres.high", dest="count.thres.high"),                                
  make_option("--pdf.output.file", dest="pdf.output.file")
)

opt <- parse_args(OptionParser(option_list=option_list), positional_arguments=TRUE, args=args)
opts <- opt$options

# ex.fea.I <- as.character(opts$exclude.features)
# print(paste("ex.fea I:", ex.fea.I))
# ex.fea.II <- strsplit(ex.fea.I, split=",")
# print(paste("ex.fea II:", ex.fea.II))
# ex.fea.III <- ex.fea.II[[1]]
# print(paste("ex.fea III:", ex.fea.III))


 seed.names.I <- as.character(opts$seed.names)
# print(paste("seed.names I:", seed.names.I))
 seed.names.II <- strsplit(seed.names.I, split=",")
# print(paste("seed.names II:", seed.names.II))
 seed.names.III <- seed.names.II[[1]]
# print(seed.names.III[1])
# print(seed.names.III[2])
# print(seed.names.III[3])

if (seed.names.III[1] == "NULL") {
    seed_names <- NULL
} else {
    seed_names <- seed.names.III
}


REVEALER.v1(ds1 = opts$ds1,
            target.name = opts$target.name,
            target.match = opts$target.match,
            ds2 = opts$ds2,
            seed.names = seed_names,
            max.n.iter = as.numeric(opts$max.n.iter),
#            identifier = opts$identifier,
#            n.perm = as.numeric(opts$n.perm),
            n.markers = as.numeric(opts$n.markers),
            locs.table.file = opts$locs.table.file,
#            exclude.features = opts$exclude.features,
#            exclude.features = ex.fea.III,            
            count.thres.low = as.numeric(opts$count.thres.low),
            count.thres.high = as.numeric(opts$count.thres.high),
            pdf.output.file = opts$pdf.output.file)

#  REVEALER.v1(ds1, target.name, target.match, ds2, seed.names, max.n.iter, identifier, n.perm, n.markers, 
#              locs.table.file, exclude.features, count.thres.low, count.thres.high, pdf.output.file)
# command line:
# <R3.0_script> <libdir>/REVEALER_library.v5C.R --ds1=<ds1> --target.name = <target.name> --target.match = <target.match> --ds2 = <ds2> --seed.names = <seed.names> --max.n.iter = <max.n.iter> --identifier = <identifier> --n.perm = <n.perm> --n.markers = <n.markers> --locs.table.file = <locs.table.file> --excludes.features = <exclude.features> --count.thres.low=<count.thres.low> --count.thres.high=<count.thres.high> --pdf.output.file = <pdf.output.file>
