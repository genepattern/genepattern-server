SOAP.logical <-
function(x)
{
  val <- as.logical(x)

  if(is.na(val)) {
   val <- as.logical(as.integer(x))
  }

  val
}  

SOAPPrimitiveConverters <-
                list("xsd:timeInstant" = as.POSIXct,
                     "xsd:float" = as.numeric,
                     "xsd:int" = as.integer,
                     "xsd:float" = as.numeric,
                     "xsd:string" = as.character,
                     "xsd:boolean" = SOAP.logical,
# Extensions from Datatypes schema                     
                     "xsd:decimal" = as.numeric)


getNodeById <-
  #
  # Find the top-level node identified by id=value of id
  # Strip the `#' prefix off `id' first.
function(id, root)
{
  id <- gsub("^#", "", id)

  for(i in xmlChildren(root)) {
    k <- xmlAttrs(i)
    if(!is.null(k) && !is.na(match("id", names(k))))
      if(id == k[["id"]])
        return(i)
  }

  return(NULL)
}


# Also need general converters that work on nodes
fromSOAP <-
  #
  # Top-level entry point to convert a top-level SOAP XML node to 
  # 
function(node, root = NULL, converters = SOAPPrimitiveConverters, append = TRUE)
{
  if(!missing(converters) && append) {
    SOAPPrimitiveConverters[names(converters)] <- converters
    converters <- SOAPPrimitiveConverters
  }
  
  if(is.character(node)) {
    root <- parseSOAP(node)
    if(xmlName(root) == "Body")
      node <- root[[1]]
    else
      node <- root
  }
  
  type <- NULL
  a <- xmlAttrs(node)
  
  if(!is.null(a)) {
    if(!is.na(match("null", names(a))))
      return(NULL)

    if(!is.na(match("type", names(a))))
      type <- a[["type"]]

    if(!is.na(match("href", names(a)))) {
      n <- getNodeById(a[["href"]], root)
      if(is.null(n))
        stop("Can't find element ", a[["href"]])
      return(fromSOAP(n, root = root, converters = converters))
    }
  }

  if(xmlName(node) == "Array" || (!is.null(type) && type == "soapenc:Array")) {
    return(fromSOAPArray(node, root = root, converters = converters))
  } else if(xmlSize(node) > 1) {
    return(fromSOAPStruct(node, root = root, converters = converters))
  } 

  if(is.null(type)) {
     # Want to check namespace is soapenc
     # and ideally don't want to prefix it with xsd:
     # but probably the most usual case is to get the typ
     # as xsd:type from the attribute and so that is the
     # default.
    type <- paste("xsd", xmlName(node), sep=":")
  }
  
  which <- match(type, names(converters))
  if(!is.na(which))
    val <- converters[[which]](xmlValue(node))
  else {
    val <- xmlValue(node)
    warning("Don't understand the SOAP type `", type, "' yet")
  }

  val
}


fromSOAPArray <-
  # Need to handle the partial arrays
  # where individual elements are  specified.
  #
function(node, type = NULL, root = NULL, converters = SOAPPrimitiveConverters)
{
  len <- xmlSize(node)
  a <- xmlAttrs(node)  

  if(missing(type)) {
    type <- a[["arrayType"]]
    if(!is.null(type)) {
      els <- strsplit(type, ":")[[1]]
      type <- gsub("\(.*\)\\\[.*", "\\1", els[2])
      len <- as.integer(gsub(".*\\\[\([0-9,]+\)\\\]", "\\1", els[2]))
      cat("Type:", type, " & length = ", len, "\n")
    }
  }


   # This is the general mechanism for dealing with offsets.
   # It doesn't work in S-Plus since it uses lexical scoping.
   # Need an OOP object for that.
  offset <- 1 
  if(!is.na(match("offset", names(a)))) {
    tmp <- gsub("\\\[\([0-9]+\)\\\]", "\\1", a[["offset"]])
    offset <- as.integer(tmp)
  }

  ans <- vector("list", len)
  val <- xmlApply(node, function(x, type = NULL, root = NULL, converters = SOAPPrimitiveConverters) {
                           z <- fromSOAP(x, root = root, converters = converters)

                           a <- xmlAttrs(x)
                           if(!is.null(a) && !is.na(match("position", names(a)))) {
                             offset <- as.integer(a[["position"]]) + 1
                           }                           

                           ans[[offset]] <<- z
                            # Do we need to differentiate between this coming from
                            # the position or the global offset.
                            # In the case of position, each item will provide 
                            # its own position value.
                           offset <<- offset + 1
                           z
                        }, type=type, converters = converters, root = root)

  return(ans)
}

fromSOAPStruct <-
function(node, root = root, converters = SOAPPrimitiveConverters)
{

   # See if there is a type="value" attribute which we will use
   # for the class.
  a <- xmlAttrs(node)  
  if(!is.null(a) && !is.na(match("type", names(a)))) {
    typeName <- a[["type"]]
  } else
    typeName <- xmlName(node)

  # Now lookup the converters to see if there is an appropriate
  # handler for this type.

  # Otherwise, just use the default mechanism.
  val <- xmlApply(node, fromSOAP, root = root, converters = converters)

  class(val) <- gsub("^[a-zA-Z]+:", "", typeName)
  
  val
}  

require(methods)

setClass("SOAPEnvelope",
          representation())

setClass("SOAPActor", representation())

setClass("SOAPFault",
          representation(message="character",
                         actor="SOAPActor",
                         detail="ANY"))

setClass("SOAPVersionMismatchFault", representation("SOAPFault"))

setClass("SOAPMustUnderstandFault", representation("SOAPFault"))

setClass("SOAPClientFault", representation("SOAPFault"))
setClass("SOAPServerFault", representation("SOAPFault"))

setClass("SOAPGeneralFault", representation("SOAPFault", code="character"))



setClass("SOAPServer", representation(
                                      host = "character",
                                      port = "integer",
                                      url = "character"),
                       prototype = list(host = "", port = as.integer(80), url="" ))

setClass("HTTPSOAPServer", representation("SOAPServer"))

setClass("FTPSOAPServer", representation("SOAPServer"),
           prototype = list(host = "", port = as.integer(21), url="" ))

# XXX: JL: added missing [["Body"]][["Fault"]] dereferences to fault trace
SOAPFault <-
function(node)
{
 faultClassNames <- c("Client"="SOAPClientFault",
                      "Server"="SOAPServerFault",
                      "MustUnderstand" = "SOAPMustUnderstandFault",
                      "VersionMismatch" = "SOAPVersionMismatchFault")        
  
 if(is.character(node)) {
   node <- parseSOAP(node)
 }
  
 code <- xmlValue(node[["Body"]][["Fault"]][["faultcode"]])
 els <- strsplit(code, ":")[[1]]
   # Check that the namespace of the fault is appropriate for us to look
   # for a built-in fault.
  els[1] <- toupper(els[1])

  if(els[1] != "SOAP" && els[1] != "soapenv")
    code <- NULL
  else
    code <- els[2]

 if(!is.null(code) && !is.na(match(code, names(faultClassNames)))) {
  className <- faultClassNames[[code]]
 } else
  className <- "SOAPGeneralFault"
 
 f <- new(className)

 f@message <- xmlValue(node[["Body"]][["Fault"]][["faultstring"]])

 if(!is.null(node[["Body"]][["Fault"]][["actor"]]))
   f@actor <- xmlValue(node[["Body"]][["Fault"]][["actor"]]) 

 f@detail <- node[["Body"]][["Fault"]][["detail"]]

 f
}  

getHTTPContent <-
  #
  # Reads the lines from the HTTP response 
  # header and converts them into a
  # name=value  collection as a character vector.
  #
function(lines)
{
 if(length(lines) < 1)
   return(NULL)

  # Not able to use read.dcf() here since we would have to write
  # these lines to a file. Use a textConnection?
  # header <- paste(lines[1:(b[1]-1)], collapse="\n")
  # header <- read.dcf(header) 
 b <- (1:length(lines))[lines  == ""]

 els <- sapply(lines[2:(b[1]-1)], function(x) strsplit(x, ":"))
 header <- sapply(els, function(x) x[2])
 names(header) <- sapply(els, function(x) x[1]) 

 els <- strsplit(lines[1], " ")[[1]] 
 header[["status"]] <- as.integer(els[2])
 header[["statusMessage"]] <- els[3]
 
  # Now paste the content or payload of the HTTP communication
 content <- paste(lines[(b[1]+1):length(lines)], collapse="\n")

 return(list(header = header, content = content))
}

isHTTPError <-
  #
  # Looks at the elements of the HTTP response
  # header, assumed to be pre-processed into name-value pairs,
  # and figures out whether the response indicates an error
  # or success.
  # It does this by looking for the non name-value pair
  # corresponding to the `HTTP/1.1 status message' string.
  # It then gets the status and compares it to the value 200.
function(response)
{
  response[["status"]] != 200
}  

parseSOAP <-
function(xmlSource, header = FALSE, reduce = FALSE, ...)
{
   require(XML)
#JL: added ", replaceEntities = TRUE"
   node <- xmlRoot(xmlTreeParse(xmlSource, ..., replaceEntities = TRUE))
   if(reduce) {
     if(xmlName(node) == "Envelope")
       node <- node[[1]]

     if(xmlName(node) == "Body")
       node <- node[[1]]
   }

   node
}

# Create XML to invoke a SOAP method

toSOAP <-
function(obj, con = xmlOutputBuffer(header=""))
{

 writeTypes <- function(x) {
      types <- getSOAPType(x)
      if(!is.null(types)) {
       for(a in names(types)) {
         cat(a,"=\"", types[[a]],"\" ", sep="", file=con)
       }
      }   
 }
  
 # con$add("value")
 if(length(obj) > 1) {
  if(!is.null(names(obj))) {
    for(i in names(obj)) {
      cat("<", i, file = con, sep="")
      writeTypes(obj[[i]])
      cat(obj[[i]])
    }
  } else {
    for(i in obj) {
      cat("<item", file=con)
      writeTypes(i)
      cat(">\r\n", file=con)
      cat(obj, file=con)
      cat("\r\n</item>", file=con)      
    }
  }
 } else {
    cat(obj, file=con)
 }

 invisible(TRUE)
}  

SOAPTypes <-
  list("character" = c("xsi:type" = "xsd:string"),
       "numeric" = c("xsi:type" = "xsd:float"),
       "double" = c("xsi:type" = "xsd:float"),       
       "integer" = c("xsi:type" = "xsd:int"),
       "logical" = c("xsi:type" = "xsd:boolean"),
       "NULL" = c("xsi:null" = 1)
    )

getSOAPType <-
function(obj)
{
  if(is.null(obj) || length(obj) == 0)
    return(SOAPTypes[["NULL"]])

  if(length(obj) > 1) {
    # Array
    n <- length(obj)
    same <- sapply(obj, function(x, target) typeof(x) == target, target = typeof(obj[[1]]))

    ans <- c("soapenc:Array")
    if(all(same)) {
      type <- getSOAPType(obj[[1]])
      ans[["soapenc:arrayType"]]  = paste(type[1], "[", n, "]", sep="")
    }
    
  } else {
     m <- typeof(obj)
     ans <- SOAPTypes[[m]]
  }
  
  return(ans)  
}  

.SOAPAction <-
function(action, method, server, xmlns)
{
  paste(action, "#", method, sep="")
}

.SOAPDefaultHandlers <-
  list(action = .SOAPAction,
       result = function(xmlSource, header, method, server) {
 # JL: reduce = FALSE
         response <- parseSOAP(xmlSource, asText = TRUE, reduce = FALSE)
 # JL: ???
         fromSOAP(response[[1]])
       })

SOAPHandlers <-
function(..., include = character(0), exclude = character(0))
{  
 defaults <- .SOAPDefaultHandlers
 els <- list(...)

 .merge(els, defaults, include, exclude)
}

.SOAP <-
function(server, method, ..., action, nameSpaces = SOAPNameSpaces(), xmlns = NULL,
          handlers = SOAPHandlers(), debug = FALSE)  
{
  # Get the connection.
 if(debug) {
   con <- textConnection(".SOAPTest", "w")
 } else {
   con <- socketConnection(server@host, port = server@port, open = "w+", blocking = TRUE)
 }
 on.exit(close(con))

 if(is.null(xmlns))
   xmlns <- c(namesp1 = action)

 if(!is.null(handlers) && !is.na(match("action", names(handlers)))) 
   action <- handlers[["action"]](action, method, server, xmlns)
 
 writeSOAPHeader(server@url, server@host, action, con)

 txt <- writeSOAPMessage(con, nameSpaces, method, ..., xmlns = xmlns)

 cat("User-agent: R 1.6.1\r\n", sep="", file=con)
 cat("Content-Length: ", nchar(txt),"\r\n", sep="", file=con)
 cat("\r\n", file=con)
 cat(txt, sep="\n", file=con)

 cat("\n", file=con)
 
 if(debug) {
   return(.SOAPTest)
 }


  # Now read the response from the server.
 txt <- readLines(con) 

  # Parse this into the HTTP header and the payload. 
 content <- getHTTPContent(txt)

  # Check things were ok with the HTTP.
 if(isHTTPError(content$header)) {
     # This would be the exception that we throw if we had an exception system.
   fault <- SOAPFault(parseSOAP(content$content, asText = TRUE))
    # 
   stop("Error occurred in the HTTP request", fault@message, fault@detail)
   return(fault)
 }

 if(!is.null(handlers) && !is.na(match("result", names(handlers))))
   return(handlers[["result"]](content$content,  content$header, method))

 return(content)
}


writeSOAPHeader <-
function(url, host, action, con, ...)
{
 # JL: HTTP 1.0, not 1.1, to avoid extra status codes in returned result from Axis/Apache
 cat("POST /", gsub("^/", "", url), " HTTP/1.0\r\n", sep="", file=con)
 cat("Host: ", host, "\r\n", sep="", file=con)
 cat("Content-Type: text/xml; charset=utf-8\r\n", file=con)
 if(!is.null(action))
   cat("SOAPAction: \"", action, "\"\r\n", sep="", file=con)

 args <- list(...)
 for(i in names(args)) {
    cat(i, ": ", args[[i]], sep="", file=con)
 }

}  


writeSOAPMessage <-
function(con, nameSpaces, method, ..., xmlns = NULL)
{
 con <- textConnection(".foo", open="w")
 writeSOAPEnvelope(con, nameSpaces = nameSpaces)

 writeSOAPBody(method, ..., xmlns = xmlns, con = con)

 cat("</soapenv:Envelope>\r\n", file=con)

  # Flushing to make certain the commands go to the server.
 cat("\r\n", file=con)
 close(con)
 paste(get(".foo"), collapse="\n")
}  


#SOAP 1.1
#      "soapenc" = 'http://schemas.xmlsoap.org/soap/encoding/',
#      "xsd" = 'http://www.w3.org/1999/XMLSchema',
# SOAP  1.2 
#      "soapenv" = 'http://www.w3.org/2002/06/soapenvelope',
#      "soapenc" = 'http://www.w3.org/2002/06/soapencoding',
#      "xsd" = 'http://www.w3.org/2001/XMLSchema',

.SOAPDefaultNameSpaces <-
  list("1.1" = 
         c(
           'soapenc'="http://schemas.xmlsoap.org/soap/encoding/",
           'soapenv'="http://schemas.xmlsoap.org/soap/envelope/",           
           'xsi'="http://www.w3.org/1999/XMLSchema-instance",
           'xsd'="http://www.w3.org/1999/XMLSchema"
           )  ,
       "1.2" = 
         c(
           'soapenc'="http://schemas.xmlsoap.org/soap/encoding/",
           'soapenv'="http://schemas.xmlsoap.org/soap/envelope/",
           'xsi'="http://www.w3.org/2001/XMLSchema-instance",
           'xsd'="http://www.w3.org/2001/XMLSchema"
           )  )       
       

.merge <-
function(els, defaults, include = NULL, exclude = NULL)
{
 if(length(els) > 0) {
   which <- match(names(defaults), names(els))
   if(any(!is.na(which)))
     els[names(defaults)[!is.na(which)]] <- defaults[!is.na(which)]
 } else
   els <- defaults

 if(length(include)) {
   els <- els[include]
 } else if(length(exclude)) {
   which <- match(exclude, names(els))
   if(any(!is.na(which)))
     els <- els[- (which[!is.na(which)])]
 }

 els   
}  

SOAPNameSpaces <-
function(..., include = character(0), exclude = character(0), version = "1.1")
{
  defaults <- .SOAPDefaultNameSpaces[[version]]
  els <- sapply(list(...), as.character)
  .merge(els, defaults, include, exclude)
}

  # This and writeSOAPBody should use the XMLOutput.. classes in the XML package.
writeSOAPEnvelope <-
function(con, nameSpaces = .SOAPNameSpaces())
{
  # JL: change apostrophes to quotes in the line below
  cat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n", file = con)
  cat("<soapenv:Envelope\r\n", file=con)
  for(i in names(nameSpaces)) {
     cat(" xmlns:", i, "=\"", nameSpaces[i],"\"\r\n", sep="", file=con)
  }
  cat(' soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"', file=con)
  cat(">\r\n", file=con)
}


writeSOAPBody <-

function(method, ..., xmlns= NULL, con)
{
 cat('<soapenv:Body>\r\n', file=con)

 nsName <- ""
 if(!is.null(xmlns) && !is.null(names(xmlns))) {
   nsName <- paste(names(xmlns)[1], ":", sep="")
 }
 
 cat("<", nsName, method, sep="", file=con)  

 if(!is.null(xmlns) && xmlns != "") {
   ns <- ""
   if(!is.null(names(xmlns)))
     ns <-  paste(":", names(xmlns)[1], sep="")
   cat(" xmlns", ns,"=\"", xmlns,"\"", file=con, sep="")
 }
 cat(">", file=con)

 args <- list(...)
 argNames <- names(args)
 
# JL: allow no-argument method calls
 if (length(args) > 0)
 for(i in 1:length(args)) {
   
  type <- getSOAPType(args[[i]])
  if(!is.null(argNames))
    argName <- argNames[i]
  else
    argName <- letters[i]

  cat("<", argName, " ", sep="", file=con)
  if(type != "") 
    cat("xsi:type=\"", type, "\" ", file=con, sep="")
  cat(">", file=con)
  toSOAP(args[[i]], con)
  cat("</", argName, ">\r\n", sep="", file=con)  
 }

 cat("</", nsName, method, ">\r\n", sep="", file=con) 

 cat("</soapenv:Body>\r\n", file=con) 
}  

#
# x <- SOAPServer(url = "TimeService/TimeService.asmx", host = "www.nanonull.com")
# SOAPServer("www.omegahat.org", "/foo")
#
SOAPServer <-
function(host, url, port = 80,  s = new(className))
{

 if(missing(url)) {
   url <- gsub("^[fth]+tp:[/]*.[a-zA-Z.0-9]+\(/.*\)", "\\1", host)
   host <- gsub("\(^[fth]+tp:[/]*.[a-zA-Z.0-9]+\)/.*", "\\1", host)   
   if(url == host)
     url <- ""
 }
  
 if(length(grep("^http", host))) {
    className <- "HTTPSOAPServer"
 } else  if(length(grep("^ftp", host))) {
    className <- "FTPSOAPServer"
 } else
    className <- "SOAPServer"
         
 host <- gsub("^http://", "", host)
 host <- gsub("^ftp://", "", host) 
 
 s@host <- host

 s@url <- url

 if(!missing(port))            
   s@port <- as.integer(port)
 
 s
}  
