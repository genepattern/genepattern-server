<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page import="org.genepattern.server.webservice.server.ProvenanceFinder,
                 org.genepattern.server.webservice.server.ProvenanceFinder.ProvenancePipelineResult"
         session="false" contentType="text/html" language="Java" %>
<%

    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String userID = (String) request.getAttribute("userID"); // get userID but don't force login if not defined

    String filename = request.getParameter("filename");


    String pipename = request.getParameter("pipelinename");

    out.println("File = " + filename);
    out.println("Pipe = " + pipename);

    ProvenanceFinder pf = new ProvenanceFinder(userID);
    
    ProvenancePipelineResult ppr = pf.createProvenancePipeline(filename, pipename);
    String lsid="";
    if (ppr != null) {
        lsid = ppr.getLsid();
    }

    response.sendRedirect("pipeline/index.jsf?lsid=" + lsid);
%>


