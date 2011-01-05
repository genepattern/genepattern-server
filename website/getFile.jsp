<%@ page
import="org.genepattern.server.webservice.server.DirectoryManager,
        org.genepattern.server.PermissionsHelper,
        org.genepattern.server.webapp.jsf.AuthorizationHelper,
        org.genepattern.util.GPConstants, 
        java.io.BufferedInputStream, 
        java.io.File,
        java.io.FileInputStream,
        java.io.InputStream,
        java.io.OutputStream" %><%
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
    boolean isAdmin = false;
    if (userID != null) {
        isAdmin = AuthorizationHelper.adminJobs(userID);
    }


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

            // check whether the current user can read the file
            if (in.exists()) {
                if (!filename.startsWith(prefix)) {
			        boolean canRead = false;
			        PermissionsHelper perm = new PermissionsHelper(isAdmin, userID, jobNumber);
			        canRead = perm.canReadJob();
			        if (!canRead) {
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
    String name = in.getName();
    //handle special case for Axis
    if (name.startsWith("Axis")) {
        int idx = name.indexOf(".att_");
        if (idx >= 0) {
            idx += ".att_".length();
            name = name.substring(idx);
        }
    }
    response.setHeader("Content-disposition", "inline; filename=\"" + name + "\"");
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
        int bytesRead = -1;
        while ((bytesRead = is.read(b)) != -1) {
            os.write(b, 0, bytesRead);
        }
    } 
    finally {
        if (is != null) {
            is.close();
        }
    }
    out.clear();
    out = pageContext.pushBody();
%>