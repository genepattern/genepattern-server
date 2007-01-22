<%@ page
        import="org.genepattern.server.webservice.server.DirectoryManager, org.genepattern.util.GPConstants, java.io.BufferedInputStream, java.io.File, java.io.FileInputStream, org.genepattern.server.util.IAuthorizationManager, org.genepattern.server.util.AuthorizationManagerFactoryImpl,java.io.InputStream, java.io.OutputStream" %>
<%

    String taskName = request.getParameter("task");
    if (taskName == null) {
        out.println("no such task: " + taskName);
        return;
    }
    String filename = request.getParameter("file");
    if (filename == null) {
        out.println("no such file: " + filename);
        return;
    }
    int i = filename.lastIndexOf(File.separator);
    if ((i != -1) && (taskName.trim().length() != 0)) {
        filename = filename.substring(i + 1); // disallow absolute paths
    }
    File in = null;
	String userID = (String) request.getAttribute(GPConstants.USERID);
	if (userID == null) userID = (String) request.getParameter(GPConstants.USERID);

	if (userID == null){ // no anonymous files 
		((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
	}
    try {
        if (taskName.length() > 0) {
            in = new File(DirectoryManager.getTaskLibDir(taskName, taskName, userID), filename);
        } else {
            // look in temp for pipelines run without saving
            in = new File(System.getProperty("java.io.tmpdir"), filename);

			// now we need to check whether this is the user or an admin trying
			// to look at the file if it exists
			if (in.exists()){
			    String prefix = userID + "_";
				if (!filename.startsWith(prefix)){
					IAuthorizationManager authManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();
					boolean isAdmin = authManager.checkPermission("administrateServer",userID );
					if (!isAdmin){
						System.out.println("SECURITY ALERT: " + userID +" tried to snoop someone elses file " + filename);
						in = File.createTempFile("dummy",null);
					}
				} 			
			}	
        }
    } catch (Exception e) {
        try {
            in = new File(DirectoryManager.getTaskLibDir(taskName, null, userID), filename);
        } catch (Exception e2) {
            out.println("No such task " + taskName);
            return;
        }
    }
    if (!in.exists()) {
        out.println("no such file: " + filename);
        return;
    }
    response.setHeader("Content-disposition", "inline; filename=\""
            + in.getName() + "\"");
    response.setHeader("Content-Type", "application/octet-stream");
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);
    response.setDateHeader("X-lastModified", in.lastModified());
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
        if (os != null) {
          //  os.close();
        }
        if (is != null) {
            is.close();
        }
    }
    out.clear();
    out = pageContext.pushBody(); 
%>