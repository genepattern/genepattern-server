.onLoad <- function (libname, pkgname) {
	require ("rJava")
	.jpackage(pkgname)
	
	# Check that the java version is 1.5. or higher
	jvmVersion = .jcall ("java/lang/System", "S", "getProperty", "java.version")
	version <- strsplit(jvmVersion, "\\.")[[1]]
	major.version = version[1]
	if (major.version < 2) {
		minor.version = version[2]
		if(minor.version < 5) {
			cat('You are using the wrong version of Java. Java version 1.5 or higher is required.\n')
		}
	}
}

close.heatmap <- function(heatmapObj) {
	return(.jcall(heatmapObj, "V", "close"))
}

get.selected.rows <- function(heatmapObj) {
	return(.jcall(heatmapObj, "[I", "getSelectedRows"))
}

get.selected.columns <- function(heatmapObj) {
	return(.jcall(heatmapObj, "[I", "getSelectedColumns"))
}

save.heatmap.image <- function(heatmapObj, file.format="jpeg", file=NULL) {
	FORMATS <- c("jpeg", "eps", "png", "tiff", "bmp")
	format <- pmatch(file.format, FORMATS)
   if (is.na(format)) 
        stop("invalid file format")
   if (format == -1) 
        stop("ambiguous file format")
   format <- FORMATS[format]
	if(is.null(file)) {
		.jcall(heatmapObj, "V", "saveImage", format)
	} else {
		.jcall(heatmapObj, "V", "saveImage", format, file)
	}
}

heatmap2 <- function(m) {
	if(class(m)=='ExpressionSet') {
		m <- exprs(m)
		d <- .jnew("org/genepattern/heatmap/ColumnMajorOrderDataset", m, row.names(m), colnames(m))
		hm <- .jnew("org/genepattern/heatmap/RHeatMap", .jcast(d, new.class = "org/genepattern/matrix/Dataset"))
		return(hm)
	} else if(class(m) == 'matrix') {
		d <- .jnew("org/genepattern/heatmap/ColumnMajorOrderDataset", m, row.names(m), colnames(m))
		hm <- .jnew("org/genepattern/heatmap/RHeatMap", .jcast(d, new.class = "org/genepattern/matrix/Dataset"))
		return(hm)
	} else {
		stop("Parameter 'm' must be a matrix or an ExpressionSet")
	}
}