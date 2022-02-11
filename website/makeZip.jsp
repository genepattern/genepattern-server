<%@ page
        import="org.genepattern.webservice.TaskInfo,
                org.genepattern.server.process.ZipTask,
                org.genepattern.server.process.ZipTaskWithDependents,
                org.genepattern.server.genepattern.GenePatternAnalysisTask,
                java.io.*"
        session="false" language="Java" %>
<%--
  ~ Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.  --%>

<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);
    String name = request.getParameter("name");
    if (name == null || name.length() == 0) {
        out.println("Must specify module name as name parameter.");
        return;
    }
    File zipFile = null;
    BufferedInputStream is = null;
    OutputStream os = response.getOutputStream();
    try {
        String userID = (String) request.getAttribute("userID");
        TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(name, userID);
        ZipTask zt;
        String inclDependents = request.getParameter("includeDependents");
        if (inclDependents != null) {
            zt = new ZipTaskWithDependents();
        } else {
            zt = new ZipTask();
        }
        zipFile = zt.packageTask(ti, userID);

        String contentType = "application/x-zip-compressed; name=\"" + ti.getName() + ".zip" + "\";";
        response.addHeader("Content-Disposition", "attachment; filename=\"" + ti.getName() + ".zip" + "\";");
        response.setContentType(contentType);
        is = new BufferedInputStream(new FileInputStream(zipFile));
        int bytesRead = 0;
        byte[] b = new byte[10000];
        while ((bytesRead = is.read(b)) != -1) {
            os.write(b, 0, bytesRead);
        }
    } catch (Exception e) {
        out.println("An error occurred while making the zip file.");
    } finally {
        if (zipFile != null) {
            zipFile.delete();
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException x) {
            }
        }
        os.flush();
    }
%>
