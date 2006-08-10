<%@ page
        import="org.genepattern.server.webservice.server.DirectoryManager, java.io.BufferedInputStream, java.io.File, java.io.FileInputStream, java.io.InputStream, java.io.OutputStream" %>
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
    try {
        if (taskName.length() > 0) {
            in = new File(DirectoryManager.getTaskLibDir(taskName), filename);
        } else {
            // look in temp for pipelines run without saving
            in = new File(System.getProperty("java.io.tmpdir"), filename);
        }
    } catch (Exception e) {
        try {
            in = new File(DirectoryManager.getTaskLibDir(taskName, null, null), filename);
        } catch (Exception e2) {
            out.println("No such task " + taskName);
            return;
        }
    }
    if (!in.exists()) {
        out.println("no such file: " + filename);
        return;
    }
    response.setHeader("Content-Disposition", "attachment; filename=" + in.getName() + ";");
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
            os.close();
        }
        if (is != null) {
            is.close();
        }
    }
%>