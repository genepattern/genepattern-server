
<%@ page
        import="org.genepattern.server.webservice.server.DirectoryManager, 
        org.genepattern.util.GPConstants, java.io.BufferedInputStream, java.io.File, 
        java.io.FileInputStream, org.genepattern.server.util.IAuthorizationManager, org.genepattern.server.util.AuthorizationManagerFactory,
        java.io.InputStream, java.io.OutputStream, java.net.URLDecoder, java.io.UnsupportedEncodingException, java.net.MalformedURLException" %>
<%

    String taskName = request.getParameter("task");
	try {
		if(taskName!= null) {
	    	taskName = URLDecoder.decode(taskName, "UTF-8");
		}
	} catch(UnsupportedEncodingException x) {}
	
    if (taskName == null) {
        out.println("no such task: " + taskName);
        return;
    }
    String filename = request.getParameter("file");
    try {
        filename = URLDecoder.decode(filename, "UTF-8");
    } catch(UnsupportedEncodingException x){}
    
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
	
	if (userID == null){ // no anonymous files 
	    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
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
					IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
					boolean isAdmin = authManager.checkPermission("administrateServer",userID );
					if (!isAdmin){
						System.out.println("SECURITY ALERT: " + userID +" tried to access someone else's file: " + filename);
						((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
						return;
					}
				} 			
			}	
        }
    } catch (Exception e) {
        try {
            in = new File(DirectoryManager.getTaskLibDir(taskName, null, userID), filename);
        } catch (Exception e2) {
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