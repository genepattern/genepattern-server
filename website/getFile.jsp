<%@ page
        import="org.genepattern.server.webservice.server.DirectoryManager,
                org.genepattern.server.PermissionsHelper,
                org.genepattern.server.webapp.FileDownloader,
                org.genepattern.server.webapp.jsf.AuthorizationHelper,
                org.genepattern.util.GPConstants,
                java.io.BufferedInputStream,
                java.io.File,
                org.genepattern.server.config.GpConfig,
                org.genepattern.server.config.GpContext,
                org.genepattern.server.config.ServerConfigurationFactory,
                org.genepattern.server.webapp.jsf.UIBeanHelper,
                java.io.FileInputStream,
                java.io.InputStream,
                java.io.OutputStream" %>
<%--
  ~ Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.  --%>

<%
    GpContext gpContext = GpContext.getServerContext();
    GpConfig gpConfig = ServerConfigurationFactory.instance();
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
        } catch (NumberFormatException e) {
            out.println("Invalid job parameter, job=" + job);
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
        } else {
            String prefix = userID + "_";
            // look in temp for pipelines run without saving
            in = new File(gpConfig.getTempDir(gpContext), filename);

            // look for file among the user uploaded files
            if (!in.exists()) {
                try {
                    GpContext context = GpContext.getContextForUser(userID);
                    File userUploadDir = ServerConfigurationFactory.instance().getUserUploadDir(context);
                    in = new File(userUploadDir, filename);
                } catch (Throwable t) {
                    //TODO: log exception
                }
            }

            //special case for Axis
            if (!in.exists()) {
                File soapAttachmentDir = gpConfig.getSoapAttDir(gpContext);
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
                        System.out.println("SECURITY ALERT: " + userID + " tried to access someone else's file: " + filename);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                }
            }
        }
    } catch (Throwable e) {
        try {
            in = new File(DirectoryManager.getTaskLibDir(taskName, null, userID), filename);
        } catch (Throwable e2) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }
    if (!in.exists()) {
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    //use same downloader as JobResultsServlet
    boolean serveContent = true;
    FileDownloader.serveFile(this.getServletContext(), request, response, serveContent, in);
    out.clear();
    out = pageContext.pushBody();
%>
