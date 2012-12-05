<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="org.genepattern.server.webapp.jsf.AuthorizationHelper" %>
<link href="<%=request.getContextPath()%>/css/style.css" rel="stylesheet" type="text/css" />
<link href="/gp/css/menu.css" type="text/css" rel="stylesheet" />
<script type="text/javascript" language="javascript">
  var contextRoot = "<%=request.getContextPath()%>/";
  <% String username = (String) request.getAttribute("userid"); %>
  var createTaskAllowed = <%= AuthorizationHelper.createModule(username) %>;
  var createPublicPipelineAllowed = <%= AuthorizationHelper.createPipeline(username) %>;
  var createPublicSuiteAllowed = <%= AuthorizationHelper.createSuite(username) %>;
  var createPrivatePipelineAllowed = <%= AuthorizationHelper.createPipeline(username) %>;
  var createPrivateSuiteAllowed = <%= AuthorizationHelper.createSuite(username) %>;
  var adminServerAllowed = <%= AuthorizationHelper.adminServer(username) %>;
  var genomeSpaceLoggedIn = false;
  var userLoggedIn = !("${requestScope.userID}" === "null" || "${requestScope.userID}" === "");
</script>
<script src="/gp/js/jquery/jquery-1.7.1.js" type="text/javascript"></script>
<script src="/gp/js/jquery/jquery-ui-1.8.16.js" type="text/javascript"></script>
<script src="/gp/js/jquery/ddsmoothmenu.js" type="text/javascript"></script>
<script src="/gp/js/menu.js" type="text/javascript"></script>
