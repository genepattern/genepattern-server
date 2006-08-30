<%@ page
        import="org.genepattern.server.genepattern.GenePatternAnalysisTask, org.genepattern.util.StringUtils, java.io.BufferedInputStream, java.io.File, java.io.FileInputStream, java.io.InputStream, java.io.OutputStream" %>
<% /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/

try {
    String tempDir = request.getParameter("job");
    if (tempDir == null) {
        tempDir = request.getParameter("dirName");
    }
    String filename = request.getParameter("filename");
    boolean errorIfNotFound = (request.getParameter("e") != null);
    if (tempDir == null || filename == null) {
        out.println("missing input parameter(s)");
        return;
    }
    if (request.getParameter("abs") != null) {
        // just strip off the /temp prefix and get the job number
        tempDir = new File(tempDir).getName();
    }
    filename = new File(filename).getName();
    File in = new File(GenePatternAnalysisTask.getJobDir(tempDir), filename);
    if (!in.exists()) {
        if (errorIfNotFound) {
            response.sendError(javax.servlet.http.HttpServletResponse.SC_GONE);
            return;
        } else {
            out.println("Unable to locate " + StringUtils.htmlEncode(filename) + " for job " + tempDir +
                    ". It may have been deleted already.");
        }
        return;
    }

    int dotIndex = in.getName().lastIndexOf(".");
    boolean saveAsDialog = true;
    if (dotIndex != -1) {
        String extension = in.getName().substring(dotIndex + 1);
        if (extension.equalsIgnoreCase("html") || extension.equalsIgnoreCase("htm")) {
            saveAsDialog = false; // view in browser
        }
    }
    if (saveAsDialog) {
        response.setHeader("Content-Disposition", "attachment; filename=" + in.getName() + ";");
    }
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
           // os.close();
        }
        if (is != null) {
            is.close();
        }
    }

} catch (Exception e){
	//e.printStackTrace();
}
out.clear();
out = pageContext.pushBody(); 
%>