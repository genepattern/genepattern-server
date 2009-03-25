
<%@ page
        import="org.genepattern.server.webservice.server.DirectoryManager, org.genepattern.server.PermissionsHelper,
        org.genepattern.util.GPConstants, java.io.BufferedInputStream, java.io.File,
        java.io.FileInputStream,org.genepattern.server.webapp.jsf.AuthorizationHelper,java.io.InputStream,java.io.OutputStream,java.net.URLDecoder,java.io.UnsupportedEncodingException,java.net.MalformedURLException" %>
<%
	String taskName = request.getParameter("task");
    if (taskName == null) {
        taskName = "";
    }

    String filename = request.getParameter("file");
    if (filename == null) {
        out.println("no such file: " + filename);
        return;
    }
    
    //if the jobNumber is set, it means that this file is being requested
    //    as a link to an input file to the given job
    //    use this information to validate group access permissions
    int jobNumber = -1;
    String job = request.getParameter("job");
    if (job != null && !"".equals(job)) {
        try {
            jobNumber = Integer.parseInt(job);
        }
        catch (NumberFormatException e) {
            out.println("Invalid job parameter, job="+job);
        }
    }

    File in = null;
	String userID = (String) session.getAttribute(GPConstants.USERID);

	if (userID == null) { // no anonymous files
	    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
		return;
	}
    try {
        if (taskName.length() > 0) {
            in = new File(DirectoryManager.getTaskLibDir(taskName, taskName, userID), filename);
        } 
        else {
            String prefix = userID + "_";
            // look in temp for pipelines run without saving
            in = new File(System.getProperty("java.io.tmpdir"), filename);
            
            //special case for Axis
            if (!in.exists()) {
                File soapAttachmentDir = new File(System.getProperty("soap.attachment.dir"));
                in = new File(soapAttachmentDir, filename);
                if (in.exists()) {
                    //authorization check
                    prefix = userID + "/";
                }
            }

            // now we need to check whether this is the user or an admin trying
            // to look at the file if it exists
            if (in.exists()) {
                if (!filename.startsWith(prefix)) {
			        boolean isAdmin = AuthorizationHelper.adminJobs(userID);
			        boolean canRead = false;
			        if (!isAdmin) {
			            //check for group access permissions
			            PermissionsHelper perm = new PermissionsHelper(userID, jobNumber);
			            canRead = perm.canReadJob();
			        }
			        if (!isAdmin && !canRead) {
			            System.out.println("SECURITY ALERT: " + userID +" tried to access someone else's file: " + filename);
		                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
		                return;
			        }
		        }
 	        }
        }
    } 
    catch (Exception e) {
        try {
            in = new File(DirectoryManager.getTaskLibDir(taskName, null, userID), filename);
        } 
        catch (Exception e2) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }
    if (!in.exists()) {
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    response.setHeader("Content-disposition", "inline; filename=\""
            + in.getName() + "\"");
    response.setHeader("Content-Type", "application/octet-stream");
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);
    response.setDateHeader("Last-Modified", in.lastModified());
    response.setHeader("Content-Length", "" + in.length());

    OutputStream os = response.getOutputStream();
    InputStream is = null;
    try {
        is = new BufferedInputStream(new FileInputStream(in));
        byte[] b = new byte[10000];
        int bytesRead;
        while ((bytesRead = is.read(b)) != -1) {
            os.write(b, 0, bytesRead);
        }
    } finally {
        if (is != null) {
            is.close();
        }
    }
    out.clear();
    out = pageContext.pushBody();
%>
