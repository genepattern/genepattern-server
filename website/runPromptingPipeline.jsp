<!-- /*
The Broad Institute
SOFTWARE COPYRIGHT NOTICE AGREEMENT
This software and its documentation are copyright (2003-2006) by the
Broad Institute/Massachusetts Institute of Technology. All rights are
reserved.

This software is supplied without any warranty or guaranteed support
whatsoever. Neither the Broad Institute nor MIT can be responsible for its
use, misuse, or functionality.
*/ -->


<%@ page import="org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.FileItemFactory,
                 org.apache.commons.fileupload.disk.DiskFileItemFactory,
                 org.apache.commons.fileupload.servlet.ServletFileUpload,
                 org.apache.commons.fileupload.servlet.ServletRequestContext,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.util.GPConstants,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo,
                 javax.servlet.RequestDispatcher,
                 java.io.File,
                 java.io.PrintWriter,
                 java.io.StringWriter,
                 java.net.InetAddress,
                 java.util.*"
         session="false" contentType="text/html" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

%>

<html>
<head>
    <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
    <link href="skin/favicon.ico" rel="shortcut icon">
    <title>Running Task</title>
</head>

<body>

<%
    /**
     * To run a visualizer, we first upload the files here to get them a URL and then call
     * runVisualizer.jsp to actually launch it after we move the params out of the smart upload object into
     * a normal request
     */


    String userID = null;

    try {

        Map requestParameters = new HashMap();

        if (ServletFileUpload.isMultipartContent(new ServletRequestContext(request))) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            List items = upload.parseRequest(request);
            File dir = File.createTempFile("pipeline", null);
            Map name2FileItem = new HashMap();
            for (Iterator iter = items.iterator(); iter.hasNext();) {
                FileItem item = (FileItem) iter.next();
                String name = item.getFieldName();
                name2FileItem.put(name, item);
            }
            dir.delete();
            dir.mkdirs();

            int fileIndex = 0;
            for (Iterator iter = items.iterator(); iter.hasNext();) {
                FileItem item = (FileItem) iter.next();
                String name = item.getFieldName();
                if (item.isFormField()) {
                    String value = item.getString();
                    requestParameters.put(name, value);
                } else {
                    fileIndex++;
                    String path = item.getName();
                    if (path == null || path.equals("")) {
                        FileItem shadowItem = (FileItem) name2FileItem.get("shadow" + (fileIndex - 1));
                        if (shadowItem != null) {
                            path = shadowItem.getString();
                        }
                    }
                    if (path == null || path.equals("")) {
                        continue;
                    }

                    if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("ftp:") ||
                            path.startsWith("file:")) {
                        // don't bother trying to save a file that is a URL, retrieve it at execution time instead
                        requestParameters.put(name, path); // map between form field name and filesystem name
                        continue;
                    }

                    File output = new File(dir, path);
                    item.write(output);
                    requestParameters.put(name, output.getCanonicalPath());
                }
            }
        }


        userID = (String) requestParameters.get(GPConstants.USERID);
        String RUN = "run";
        String CLONE = "clone";


        String lsid = (String) requestParameters.get("taskLSID");
        String taskName = (String) requestParameters.get("taskName");

        // set up the call to the runVisualizer.jsp by putting the params into the request
        // and then forwarding through a requestDispatcher
        TaskInfo task = GenePatternAnalysisTask.getTaskInfo(lsid, userID);
        ParameterInfo[] parmInfos = task.getParameterInfoArray();

        request.setAttribute("name", lsid);
        String server = request.getScheme() + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" +
                System.getProperty("GENEPATTERN_PORT");
        if (parmInfos == null) {
            parmInfos = new ParameterInfo[0];
        }

        ArrayList missingReqParams = new ArrayList();
        for (int i = 0; i < parmInfos.length; i++) {
            ParameterInfo pinfo = parmInfos[i];
            String value;
            if (pinfo.isInputFile()) {
                value = (String) requestParameters.get(pinfo.getName());
                if (value != null) {
                    if (!value.startsWith("http:") && !value.startsWith("https:") && ! value.startsWith("ftp:") ||
                            value.startsWith("file:")) {
                        value = server + "/" + request.getContextPath() + "/getFile.jsp?task=&file=" + value;
                    }
                }
                HashMap pia = pinfo.getAttributes();
                pia.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);

            } else {
                value = (String) requestParameters.get(pinfo.getName());
            }
            request.setAttribute(pinfo.getName(), value);

            // look for missing required params
            if ((value == null) || (value.trim().length() == 0)) {
                HashMap pia = pinfo.getAttributes();
                boolean isOptional =
                        ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
                                .length() > 0;

                if (!isOptional) {
                    missingReqParams.add(pinfo);
                }
            }

            pinfo.setValue(value);

        }

        if (missingReqParams.size() > 0) {

%>
<jsp:include page="navbar.jsp"/>
<%

    request.setAttribute("missingReqParams", missingReqParams);
    (request.getRequestDispatcher("runTaskMissingParams.jsp")).include(request, response);
%>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
            return;
        }

        request.setAttribute("PipelineParameterInfo", parmInfos);

        RequestDispatcher rd = request.getRequestDispatcher("runPipeline.jsp");
        rd.include(request, response);

    } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        out.println(sw.toString());
    }

%>


