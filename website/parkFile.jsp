<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<%@ page import="
		 org.apache.commons.fileupload.DiskFileUpload,
                 org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.FileUpload,
                 org.genepattern.util.StringUtils,
                 java.io.File,
                 java.net.*,
                 java.util.Enumeration,
                 java.util.HashMap,
                 java.util.Iterator,
                 java.util.List,
                 java.util.Vector"
         session="false" contentType="text/plain" language="Java" %><%@ page import="org.genepattern.server.config.ServerConfigurationFactory"%><%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    try {
        int i;
        File attachedFile = null;
        String attachmentName = null;

        HashMap requestParameters = new HashMap();
        int fileCount = 0;
        String username = (String) request.getAttribute("userID");
        if (username == null || username.length() == 0) {
            return; // come back after login
        }
        String agent = request.getHeader("USER-AGENT");
        boolean isIE = false;
        if (agent.indexOf("MSIE") >= 0) {
            isIE = true;
        }

        DiskFileUpload fub = new DiskFileUpload();
        boolean isEncodedPost = FileUpload.isMultipartContent(request);
        List params = fub.parseRequest(request);
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            FileItem fi = (FileItem) iter.next();
            if (fi.isFormField()) {
                requestParameters
                        .put(fi.getFieldName(), fi.getString());
            } else {
                // it is the file
                fileCount++;
                String name = fi.getName();
                if (isIE) {
                    int idx1 = name.lastIndexOf("/");
                    int idx2 = name.lastIndexOf("\\");
                    int idx = Math.max(idx1, idx2);
                    name = name.substring(idx);
                }
                if (name == null || name.equals("")) {
                    continue;
                }
                File zipFile = new File(ServerConfigurationFactory.instance().getTempDir(null), name);
                requestParameters
                        .put(fi.getFieldName(), zipFile);
                fi.write(zipFile);
            }
        }
        if (requestParameters.get("file") == null) {
		// no file to park
        return;
    }

    String fileURL = null;
    attachedFile = (File) requestParameters.get("file");
    attachmentName = attachedFile.getName();
    fileURL = attachedFile.toURI().toString();
        
    String userdirname = "user_" + username.hashCode();
	String path = userdirname +"/" + attachedFile.getName();

	File parkingLot = new File("temp/","parking");	
      if (!parkingLot.exists()){
		parkingLot.mkdir();
	} 
	File userDir = new File(parkingLot, userdirname);       
	if (!userDir.exists()){
		userDir.mkdir();
	}
	File parkedFile = new File(userDir, attachedFile.getName());    

    	// move file to parking lot            
      attachedFile.renameTo(parkedFile);

    String serverURL = System.getProperty("GenePatternURL");
	out.println(serverURL + "/getParkedFile.jsp?filename="+StringUtils.htmlEncode(attachedFile.getName()) +"&uid="+username.hashCode() );

	//out.println("" + parkedFile.getPath());
    } catch (Exception ioe) {
        ioe.printStackTrace();
    }
%>
