<%@ page
        import="org.genepattern.server.webservice.server.DirectoryManager, java.io.BufferedInputStream, java.io.File, java.io.FileInputStream, java.io.InputStream, java.io.OutputStream" %>
<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<%

    String filename = request.getParameter("filename");
    String uid = request.getParameter("uid");

    if ((filename == null) || (uid == null)) {
        out.println("no such file: " + filename);
        return;
    }

    File pl = new File("temp/", "parking");
    if (!pl.exists()) {
        out.println("no such file: " + filename);
        return;
    }
    String userdirname = "user_" + uid;

    File ud = new File(pl, userdirname);
    if (!ud.exists()) {
        out.println("no such file: " + filename);
        return;
    }

    File in = new File(ud, filename);
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
            //  os.close();
        }
        if (is != null) {
            is.close();
        }
    }
    out.clear();
    out = pageContext.pushBody();
%>
