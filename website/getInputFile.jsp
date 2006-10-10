<%@ page
        import="org.genepattern.util.GPConstants, java.io.BufferedInputStream, java.io.File, java.io.FileInputStream, java.io.InputStream, java.io.OutputStream, java.net.URLConnection, java.util.HashMap" %>
<%--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
 
--%>

<%
    String filename = request.getParameter("file");
    if (filename == null) {
        out.println("no file specified");
        return;
    }
    File inputFileDir = new File("../temp/attachments/");
    int i = filename.lastIndexOf(File.separator);
    if (i != -1) {
        filename = filename.substring(i + 1); // disallow absolute paths
    }
    String contentType = URLConnection.getFileNameMap().getContentTypeFor(filename);
    if (contentType == null) {
        HashMap htTypes = new HashMap();
        htTypes.put("." + GPConstants.TASK_TYPE_PIPELINE, "text/plain");
        htTypes.put(".cls", "text/plain");
        htTypes.put(".gct", "text/plain");
        htTypes.put(".res", "text/plain");
        htTypes.put(".odf", "text/plain");
        i = filename.lastIndexOf(".");
        String extension = (i > -1 ? filename.substring(i) : "");
        contentType = (String) htTypes.get(extension.toLowerCase());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
    }
    File in = null;
    try {
        // look in temp for pipelines run without saving
        in = new File(inputFileDir, filename);
    } catch (Exception e) {
        out.println("Problem with temporary directory");
    }
    if (!in.exists()) {
        out.println("no such file: " + filename);
        return;
    }
    response.setHeader("Content-Disposition", "attachment; filename=" + in.getName() + ";");
    response.setHeader("Content-Type", contentType);
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
    out.clear();
    out = pageContext.pushBody(); 

%>