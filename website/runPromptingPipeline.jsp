<!-- /*
The Broad Institute
SOFTWARE COPYRIGHT NOTICE AGREEMENT
This software and its documentation are copyright (2003-2006) by the
Broad Institute/Massachusetts Institute of Technology. All rights are
reserved.

This software is supplied without any warranty or guaranteed support
whatsoever. Neither the Broad Institute nor MIT can be responsible for its
use, misuse, or functionality.
*/ -->


<%@ page import="org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.disk.DiskFileItemFactory,
                 org.apache.commons.fileupload.servlet.ServletFileUpload,
                 org.apache.commons.fileupload.servlet.ServletRequestContext,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.util.GPConstants,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo,
                 javax.servlet.RequestDispatcher,
                 java.io.PrintWriter,
                 java.io.File,
                 org.apache.commons.io.FilenameUtils,
              	  org.apache.commons.fileupload.FileItem,
                 java.net.MalformedURLException,
                 java.net.URL,
                 java.util.ArrayList,
                 java.util.HashMap,
                 java.util.Iterator,
                 java.util.List,
                 java.util.Map"
         session="false" contentType="text/html" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

%>

<html>
<head>
    <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
    <link href="skin/favicon.ico" rel="shortcut icon">
    <title>Running Task</title>
</head>

<body>

<%
    /**
     * To run a pipeline with prompt when run parameters, we put parameters into the request object.
     * File parameters are stored as FileItems, all other parameters as strings
     */
    Map name2FileItem = new HashMap();
	File tempDir = File.createTempFile("runTaskPipeline", null);
	
	tempDir.delete();
	tempDir.mkdir();
    try {
        Map requestParameters = new HashMap();
        if (ServletFileUpload.isMultipartContent(new ServletRequestContext(request))) {
            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
            List items = upload.parseRequest(request);
            for (Iterator iter = items.iterator(); iter.hasNext();) {
                FileItem item = (FileItem) iter.next();
                String name = item.getFieldName();
                name2FileItem.put(name, item);
            }
            for (Iterator iter = items.iterator(); iter.hasNext();) {
                FileItem item = (FileItem) iter.next();
                String name = item.getFieldName();
                if (item.isFormField()) {
                    String value = item.getString();
                    requestParameters.put(name, value);
                } else {
                    String path = item.getName();
                    if (path == null || path.equals("")) {
                        FileItem shadowItem = (FileItem) name2FileItem.get("shadow" + name);
                        if (shadowItem != null) {
                            path = shadowItem.getString();
                        }
                    }
                    if (path == null || path.equals("")) {
                        continue;
                    }
                    requestParameters.put(name, path);
                    
                }
            }
        }
        
        
        
        String userID = (String) requestParameters.get(GPConstants.USERID);
        String lsid = (String) requestParameters.get("taskLSID");
        TaskInfo task = GenePatternAnalysisTask.getTaskInfo(lsid, userID);
        if (task == null) {
            out.println("Unable to find task " + lsid);
            return;
        }
        ParameterInfo[] parmInfos = task.getParameterInfoArray();
        request.setAttribute("name", lsid); //used by runPipeline.jsp to get pipeline to run
        if (parmInfos == null) {
            parmInfos = new ParameterInfo[0];
        }
        ArrayList missingReqParams = new ArrayList();
        for (int i = 0; i < parmInfos.length; i++) {
            ParameterInfo pinfo = parmInfos[i];
            String value;
            if (pinfo.isInputFile()) {
                value = (String) requestParameters.get(pinfo.getName());
                if (value != null) {
                    boolean isURL = false;
                    try {
                        new URL(value);
                        isURL = true;
                    } catch (MalformedURLException mfe) {
                    }
                    if (!isURL) {
                        
                        FileItem fi = (FileItem) name2FileItem.get(pinfo.getName());
                        // RunPipelineForJsp handles setting the proper value for this parameter
						String fileName = fi.getName();
                
                        fileName = FilenameUtils.getName(fileName);
                        File oldFile = new File(fileName);
                    	
                        File file = new File(tempDir, fileName);
                        if (file.exists()) {
                            if (fileName.length() < 3) {
                                fileName += "tmp";
                            }
                            file = File.createTempFile(fileName, FilenameUtils.getExtension(fileName), tempDir);
                        } 
                        try {
                           fi.write(file);
                           // deal with reload files that are not uploaded and so for which
                           // the write leaves an empty file
                           if (file.length() == 0){
                        	   file = oldFile;
                           }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                     
                       
                        request.setAttribute(pinfo.getName(), file); 
                        
                        //request.setAttribute(pinfo.getName(), value);
                        
                        
                    } else {
                        request.setAttribute(pinfo.getName(), value);
                    }
                }
            } else {
                value = (String) requestParameters.get(pinfo.getName());
                request.setAttribute(pinfo.getName(), value);
            }

            // look for missing required params
            if ((value == null) || (value.trim().length() == 0)) {
                HashMap pia = pinfo.getAttributes();
                boolean isOptional =
                        ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
                                .length() > 0;
                if (!isOptional) {
                    missingReqParams.add(pinfo);
                }
            }
            pinfo.setValue(value);
        }
        if (missingReqParams.size() > 0) {

%>
<jsp:include page="navbar.jsp"/>
<%
    request.setAttribute("missingReqParams", missingReqParams);
    (request.getRequestDispatcher("runTaskMissingParams.jsp")).include(request, response);
%>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
            return;
        }
        RequestDispatcher rd = request.getRequestDispatcher("runPipeline.jsp");
        rd.include(request, response);
    } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintWriter(out));
    }

%>


