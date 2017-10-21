message <- function (..., domain = NULL, appendLF = TRUE) {

}

#################################################################
# SurvivalCurve.R (ver.1.0)                May 4, 2007
#
#     Survival curve using clinical data & .cls file
#     Yujin Hoshida (Broad Institute)
#################################################################

### SurvivalCurve (Main) ###

SurvivalCurve <- function(
  input.surv.data.filename,
  input.cls.filename,

  output.name="SurvivalCurve",

  time.field="time",
  censor.field="status",

  cls.field="NA",

  print.fit.results="F",
                             
  # plot
  line.type.col.assign="automatic",  # alt. "manual"
  line.type.specify="NA",  # 1: solid, 2: dashed, 3: dotted, 4: dot-dashed
  line.col.specify="NA",
  line.width=1,          # 1: thin, 2: thick
  time.conversion=1,       # 1: as is, 365.25: "days to years", 12: "months to years"
  max.time=NA,
  surv.function.lower=0,
  surv.function.higher=1,
  curve.type="log",  # x, "event": 1-x, "cumhaz": -log(x)
  show.conf.interval=0, # 0: hide, 1: show
  add.legend="T",
  legend.position="left-bottom"
  )
{

  # Advanced options

  ## survfit
  survival.estimator <- "kaplan-meier"   # "kaplan-meier": Kaplan-Meier estimator
                        # "fleming-harrington": Fleming-Harrington estimator
                        # "fh2": Nelson-Aalen estimator (error="tsiatis",fun="cumhaz")
  variance <- "greenwood"     # Greenwood formula for the variance
                        # sum of terms d/(n*(n-m)), d: #deaths at a given time point,
                        # n: sum of 'weights' for all individuals still at risk,
                        # m: sum of 'weights' for the deaths, at that time.
                        # "tsiatis" when fh2/coxph 
  ##  confidence.interval=0.95,
  conf.interval.type <- "log"    # "none"
                     # "plain": +- conf.int*se(curve)
                     # "log": cumulative hazard or log(survival)
                     # "log-log": log hazard or log(-log(survival)). never extend past 0 or 1
  ## plot option
  censor.mark <- "T"
  mark.type <- 3  # 3=cross;1=open circle;2=open triangle;4=x;5=open diamond;8=asterisk;15=closed box;16=closed diamond;17=closed triangle;18=closed circle;20=dot;22=open box

  # Redefine numeric inputs for GenePattern

#  confidence.interval<-as.numeric(confidence.interval)

  if (line.type.specify == "NA" || line.type.specify == "-lt")
  {
    line.type.specify<-NA
  }
  else
     line.type.specify <- substring(line.type.specify, 4, nchar(line.type.specify))

  if (line.col.specify == "NA" || line.col.specify=="-lc")
  {
    line.col.specify<-NA
  }
  else
     line.col.specify <- substring(line.col.specify, 4, nchar(line.col.specify))

  if (line.type.col.assign=="manual" && is.na(line.type.specify))
  {
    stop("Please specify a value for manual line type")
  }

  if (line.type.col.assign=="manual" && is.na(line.col.specify))
  {
    stop("Please specify a value for manual line color")
  }

  line.width<-as.integer(line.width)
  if (censor.mark=="T"){
    censor.mark<-TRUE
  }else{
    censor.mark<-FALSE
  }
  mark.type<-as.integer(mark.type)
  time.conversion<-as.numeric(time.conversion)

  if(max.time == "-m")
  {
    max.time <- NA
  }
  else
  {    
     max.time <- substring(max.time, 3, nchar(max.time))
     max.time<-as.numeric(max.time)
  }

  surv.function.lower<-as.numeric(surv.function.lower)
  surv.function.higher<-as.numeric(surv.function.higher)
  show.conf.interval<-as.integer(show.conf.interval)

  # omitted from the command line
  confidence.interval=0.95

  # read input files
  
  surv.data<-read.delim(input.surv.data.filename,header=T)

  if(input.cls.filename != '-c')
  {
    input.cls.filename <- substring(input.cls.filename, 3, nchar(input.cls.filename))

    cls<-as.vector(t(read.delim(input.cls.filename,header=F,sep=" ",skip=2)))
    cls.label <- as.vector(rownames(table(cls)))
  }
  else if (cls.field != "-f")
  {
    if(cls.field == "NA" || cls.field =="")
        stop("You must provide either a cls file or a cls field parameter")

    cls.field <- substring(cls.field, 3, nchar(cls.field))
    eval(parse(text=paste("cls <- as.vector(surv.data$",cls.field,")",sep="")))

    if(is.null(cls))
        stop(paste("Invalid cls field: ", cls.field))
         
    cls.label <- as.vector(rownames(table(cls)))
  }
  else
    stop("You must provide either a cls file or a cls field parameter")

  num.cls<-length(table(cls))

  if(length(surv.data[,1])!=length(cls)){
    stop("### Survival data and class file don't match! ###")
  }

  surv.data<-cbind(surv.data,cls)

  eval(parse(text=paste("time <- surv.data$",time.field,sep="")))
  eval(parse(text=paste("status <- surv.data$",censor.field,sep="")))
  time <- as.numeric(time)
  status <- as.numeric(status)
  if (length(time)!=length(status)){
    stop("### Check names for survival time & censor! ###")
  }

  # load "survival" package

  require(splines,quietly=T)
  require(survival,quietly=T)

  ## Survival fit
  
  fitted=survfit(Surv(time,status)~cls,data=surv.data,type=survival.estimator,error=variance,conf.int=confidence.interval,conf.type=conf.interval.type)

  # title attached to outputs
  
  if (survival.estimator=="kaplan-meier"){
    title<-"#  Kaplan-Meier estimator"
  }
  if (survival.estimator=="fleming-harrington"){
    title<-"#  Fleming-Harrington estimator"
  }
  if (survival.estimator=="fh2"){
    title<-"#  Nelson-Aalen estimator"
  }

  if (print.fit.results=="T"){

  fit.name <- paste(output.name,"_FitSummary.txt",sep="")
  table.name <- paste(output.name,"_Table.txt",sep="")
    
    write.table(title,fit.name,quote=F,sep="\t",row.names=F,col.names=F)
    write.table(title,table.name,quote=F,sep="\t",row.names=F,col.names=F)
    write.table("",fit.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)
    write.table("",table.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)

    write.table(capture.output(fitted),fit.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)
    write.table(capture.output(summary(fitted)),table.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)
  }

  ## Plot survival curve
  # x axis range
  if (is.na(max.time)){
    x.max<-max(time)/time.conversion
  }else{
    x.max<-max.time/time.conversion
  }

  # x-y axis label
  x.axis.label="Time"
  if (time.conversion==365.25){
    x.axis.label<-"Years"
  }

  if (time.conversion==12){
    x.axis.label<-"Years"
  }
  
  y.axis.label="Survival function"
  if (curve.type=="event"){
    y.axis.label<-"Cumulative events"
  }
  if (curve.type=="cumhaz"){
    y.axis.label<-"Cumulative hazard"
  }

  if(length(grep("Cairo", installed.packages())) != 0)
  {
    library(Cairo)
    CairoPNG(paste(output.name, "_SurvivalCurve.png", sep=""), , width = 830, height = 800)
  }
  else if (capabilities("png"))
  {
    png(paste(output.name, "_SurvivalCurve.png", sep=""))
  }
  else
    pdf(paste(output.name, "_SurvivalCurve.pdf", sep=""))

  xlim <- c(0, x.max)
  ylim <- c(surv.function.lower,surv.function.higher)
  x.max <- as.double(x.max)

  if(curve.type=="cloglog")
  {
    xlim <- NULL
  }

  if (line.type.col.assign=="automatic"){
    suppressWarnings(plot(fitted, lty=1, col=c("blue","red","black","green","orange","purple"), lwd=line.width, firstx=0, mark.time=censor.mark, mark=mark.type, xscale=time.conversion, xlim=xlim, ylim=ylim, xlab=x.axis.label, ylab=y.axis.label, fun=curve.type, conf.int=show.conf.interval, main=title, cex.axis=1))
    box(lwd=line.width)
  }
    
  if (line.type.col.assign=="manual"){
    if (is.na(line.type.specify)){
        stop("### Line type is not specified! ###")
    }
    line.type.specify<-as.numeric(unlist(strsplit(line.type.specify,",")))
    line.col.specify<-unlist(strsplit(line.col.specify,","))

    suppressWarnings(plot(fitted, lty=line.type.specify, col=line.col.specify, lwd=line.width, mark.time=censor.mark, mark=mark.type, xscale=time.conversion, xlim=xlim, ylim=ylim, xlab=x.axis.label, ylab=y.axis.label, fun=curve.type, conf.int=show.conf.interval, main=title, cex.axis=1))
    box(lwd=line.width)
  }

  # legend

  if (add.legend=="T"){
    if (legend.position=="left-bottom"){
        x.legend <-"bottomleft"
    }

    if (legend.position=="left-top"){
        x.legend <-"topleft"
    }

    if (legend.position=="right-bottom"){
        x.legend <-"bottomright"
    }
    if (legend.position=="right-top"){
        x.legend <-"topright"
    }
    
    if (line.type.col.assign=="automatic"){
        legend(x=x.legend, legend=cls.label, lty=1, col=c("blue","red","black","green","orange","purple"), lwd=line.width, inset=0.02)
    }
    if (line.type.col.assign=="manual"){
        legend(x=x.legend, legend=cls.label, lty=line.type.specify, col=line.col.specify, lwd=line.width, inset=0.02)
    }
  }
  
  dev.off()

} # end of main
