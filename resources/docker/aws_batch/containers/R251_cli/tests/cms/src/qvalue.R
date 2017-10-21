compute.qvalue <- function(input.file) {
	if(!file.exists(input.file)) {
		cat(paste("Unable to compute q values:", input.file, " not found."), sep='')
	}
	values <- read.table(input.file, sep="\t", row.names=NULL, colClasses="real")
   output.file <- 'qvalues.txt'
   
	result <- qvalue(p=values[,1])
	qvalues <- result$qvalues
	
	lambda <- result$lambda
	
	pi0 <- rep(0, length(lambda))
	p2 <- result$pval[order(result$pval)]
   for (i in 1:length(lambda)) {
      pi0[i] <- mean(p2 > lambda[i])/(1 - lambda[i])
   }
   spi0 <- smooth.spline(lambda, pi0, df = 3)
   
   cat("pi0=", sep='', file=output.file, append=TRUE)
   cat(result$pi0, sep=' ', file=output.file, append=TRUE)
   cat("\n", sep='', file=output.file, append=TRUE)
   
   cat("lambda=", sep='', file=output.file, append=TRUE)
   cat(lambda, sep=" ", file=output.file, append=TRUE)
   cat("\n", sep='', file=output.file, append=TRUE)
   
   cat("pi0(lambda)=", sep='', file=output.file, append=TRUE)
   cat(pi0, sep=' ', file=output.file, append=TRUE)
   cat("\n", sep='', file=output.file, append=TRUE)
   
   cat("cubic spline(lambda)=", sep='', file=output.file, append=TRUE)
   cat(spi0$y, sep=' ', file=output.file, append=TRUE)
   cat("\n", sep='', file=output.file, append=TRUE)
   write.table(qvalues, output.file, sep="\t", col.names=FALSE, quote=FALSE, append=TRUE, row.names=FALSE)
}

        
qvalue <- function (p, lambda = seq(0, 0.95, 0.05), pi0.method = "smoother", 
    fdr.level = NULL, robust = F, gui = F) 
{
    if (min(p) < 0 || max(p) > 1) {
        if (gui) 
            eval(expression(postMsg(paste("ERROR: p-values not in valid range.", 
                "\n"))), parent.frame())
        else print("ERROR: p-values not in valid range.")
        return(0)
    }
    if (length(lambda) > 1 && length(lambda) < 4) {
        if (gui) 
            eval(expression(postMsg(paste("ERROR: If length of lambda greater than 1, you need at least 4 values.", 
                "\n"))), parent.frame())
        else print("ERROR: If length of lambda greater than 1, you need at least 4 values.")
        return(0)
    }
    if (length(lambda) > 1 && (min(lambda) < 0 || max(lambda) >= 
        1)) {
        if (gui) 
            eval(expression(postMsg(paste("ERROR: Lambda must be within [0, 1).", 
                "\n"))), parent.frame())
        else print("ERROR: Lambda must be within [0, 1).")
        return(0)
    }
    m <- length(p)
    if (length(lambda) == 1) {
        if (lambda < 0 || lambda >= 1) {
            if (gui) 
                eval(expression(postMsg(paste("ERROR: Lambda must be within [0, 1).", 
                  "\n"))), parent.frame())
            else print("ERROR: Lambda must be within [0, 1).")
            return(0)
        }
        pi0 <- mean(p >= lambda)/(1 - lambda)
        pi0 <- min(pi0, 1)
    }
    else {
        pi0 <- rep(0, length(lambda))
        for (i in 1:length(lambda)) {
            pi0[i] <- mean(p >= lambda[i])/(1 - lambda[i])
        }
        if (pi0.method == "smoother") {
            spi0 <- smooth.spline(lambda, pi0, df = 3)
            pi0 <- predict(spi0, x = max(lambda))$y
            pi0 <- min(pi0, 1)
        }
        else if (pi0.method == "bootstrap") {
            minpi0 <- min(pi0)
            mse <- rep(0, length(lambda))
            pi0.boot <- rep(0, length(lambda))
            for (i in 1:100) {
                p.boot <- sample(p, size = m, replace = T)
                for (i in 1:length(lambda)) {
                  pi0.boot[i] <- mean(p.boot > lambda[i])/(1 - 
                    lambda[i])
                }
                mse <- mse + (pi0.boot - minpi0)^2
            }
            pi0 <- min(pi0[mse == min(mse)])
            pi0 <- min(pi0, 1)
        }
        else {
            print("ERROR: 'pi0.method' must be one of 'smoother' or 'bootstrap'.")
            return(0)
        }
    }
    if (pi0 <= 0) {
        if (gui) 
            eval(expression(postMsg(paste("ERROR: The estimated pi0 <= 0. Check that you have valid p-values or use another lambda method.", 
                "\n"))), parent.frame())
        else print("ERROR: The estimated pi0 <= 0. Check that you have valid p-values or use another lambda method.")
        return(0)
    }
    if (!is.null(fdr.level) && (fdr.level <= 0 || fdr.level > 
        1)) {
        if (gui) 
            eval(expression(postMsg(paste("ERROR: 'fdr.level' must be within (0, 1].", 
                "\n"))), parent.frame())
        else print("ERROR: 'fdr.level' must be within (0, 1].")
        return(0)
    }
    u <- order(p)
    v <- rank(p)
    qvalue <- pi0 * m * p/v
    if (robust) {
        qvalue <- pi0 * m * p/(v * (1 - (1 - p)^m))
    }
    qvalue[u[m]] <- min(qvalue[u[m]], 1)
    for (i in (m - 1):1) {
        qvalue[u[i]] <- min(qvalue[u[i]], qvalue[u[i + 1]], 1)
    }
    if (!is.null(fdr.level)) {
        retval <- list(call = match.call(), pi0 = pi0, qvalues = qvalue, 
            pvalues = p, fdr.level = fdr.level, significant = (qvalue <= 
                fdr.level), lambda = lambda)
    }
    else {
        retval <- list(call = match.call(), pi0 = pi0, qvalues = qvalue, 
            pvalues = p, lambda = lambda)
    }
    class(retval) <- "qvalue"
    return(retval)
}